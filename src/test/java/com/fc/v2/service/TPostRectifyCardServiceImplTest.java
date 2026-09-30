package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.mapper.auto.TPostOutletCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyCardMapper;
import com.fc.v2.mapper.auto.TPostRectifyCardRecordMapper;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.model.auto.TPostRectifyCardRecord;
import com.fc.v2.service.impl.TPostRectifyCardServiceImpl;

/**
 * 责令整改事务卡状态机规则验证（H2 + 真实 MyBatis-Plus，不起 Spring 容器）。
 */
public class TPostRectifyCardServiceImplTest {

    private static final String OUTLET = "WD-001";

    private SqlSession sqlSession;
    private TPostOutletCardMapper outletMapper;
    private TPostRectifyCardMapper cardMapper;
    private TPostRectifyCardServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:rectify_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        DataSource dataSource = ds;
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement();
             InputStream in = getClass().getResourceAsStream("/schema-rectify-h2.sql")) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            String ddl = new String(bytes, StandardCharsets.UTF_8);
            for (String sql : ddl.split(";")) {
                if (!sql.trim().isEmpty()) {
                    st.execute(sql);
                }
            }
        }

        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(dataSource);
        SqlSessionFactory factory = fb.getObject();
        factory.getConfiguration().addMapper(TPostRectifyCardMapper.class);
        factory.getConfiguration().addMapper(TPostRectifyCardRecordMapper.class);
        factory.getConfiguration().addMapper(TPostOutletCardMapper.class);
        this.sqlSession = factory.openSession(true);
        this.cardMapper = sqlSession.getMapper(TPostRectifyCardMapper.class);
        TPostRectifyCardRecordMapper recordMapper = sqlSession.getMapper(TPostRectifyCardRecordMapper.class);
        this.outletMapper = sqlSession.getMapper(TPostOutletCardMapper.class);

        // 备案名录里先有该网点一行（bill_no 即网点号），销案时回执落到这一行
        TPostOutletCard outlet = new TPostOutletCard();
        outlet.setId(IdWorker.getId());
        outlet.setBillNo(OUTLET);
        outlet.setDelFlag(0);
        outletMapper.insert(outlet);

        this.service = new TPostRectifyCardServiceImpl();
        inject(service, "postRectifyCardMapper", cardMapper);
        inject(service, "postRectifyCardRecordMapper", recordMapper);
        inject(service, "postOutletCardMapper", outletMapper);
    }

    @AfterEach
    void tearDown() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    @Test
    void openWritesIssueStageAndRejectsDuplicateActiveCard() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        assertEquals(0, card.getStage().intValue());
        assertEquals(1, card.getStatus().intValue());

        List<TPostRectifyCardRecord> trace = service.selectActiveTrace(card.getId());
        assertEquals(1, trace.size());
        assertEquals(0, trace.get(0).getStage().intValue());
        assertTrue(trace.get(0).getContent().contains("核签单号:HC-9"));
        assertTrue(trace.get(0).getContent().contains("青屏县某路1号"));

        // 头一张没销案也没打住之前，第二张立不起来
        assertNull(service.open(OUTLET, "别处", "HC-10", new Date(), "zhang"));
    }

    @Test
    void openRejectsMissingEssentials() {
        assertNull(service.open(null, "addr", "HC-1", new Date(), "zhang"));
        assertNull(service.open(OUTLET, "addr", null, new Date(), "zhang"));
        assertNull(service.open(OUTLET, "addr", "HC-1", null, "zhang"));
    }

    @Test
    void advancesOneStageAtATimeAndClosesWithReceipt() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));

        TPostRectifyCard r1 = service.advance(card.getId(), "改了3处，2026-01-10改完", "enterprise");
        assertEquals(1, r1.getStage().intValue());
        assertEquals("改了3处，2026-01-10改完", r1.getContent());

        // 复查段缺当场点验结果不许过（进入复查这一关口拦）
        assertNull(service.advance(card.getId(), "  ", "inspector"));

        TPostRectifyCard r2 = service.advance(card.getId(), "现场核对合格", "inspector");
        assertEquals(2, r2.getStage().intValue());
        assertEquals("现场核对合格", r2.getReviewResult());

        TPostRectifyCard r3 = service.advance(card.getId(), null, "chief");
        assertEquals(3, r3.getStage().intValue());
        assertEquals(2, r3.getStatus().intValue());

        // 销案时备案名录那一行落了回执
        TPostOutletCard outlet = outletMapper.selectOne(new QueryWrapper<TPostOutletCard>()
                .eq("bill_no", OUTLET));
        assertEquals(card.getBizNo(), outlet.getRectifyReceipt());

        // 已销案：不能再往前、不能再退
        assertNull(service.advance(card.getId(), "x", "chief"));
        assertNull(service.rollback(card.getId(), "chief"));
        assertEquals(4, service.selectActiveTrace(card.getId()).size());
    }

    @Test
    void reviewNotesOverdueWhenDeadlinePassed() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() - 3600000L));
        service.advance(card.getId(), "改完", "enterprise");
        TPostRectifyCard r = service.advance(card.getId(), "现场核对", "inspector");
        TPostRectifyCardRecord review = service.selectActiveTrace(card.getId()).get(2);
        assertTrue(review.getContent().contains("已过责令改正期限"));
        assertEquals(2, r.getStage().intValue());
    }

    @Test
    void terminateStopsTheCardForever() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        service.advance(card.getId(), "整改中", "enterprise");

        TPostRectifyCard stopped = service.advance(card.getId(),
                ITPostRectifyCardService.ACTION_TERMINATE, "复查不再往下走", "inspector");
        assertEquals(3, stopped.getStatus().intValue());

        // 事后谁也不能再往它后面接一段，也退不回去
        assertNull(service.advance(card.getId(), "x", "inspector"));
        assertNull(service.rollback(card.getId(), "inspector"));
        // 打住不许多落一句记事：在屏履历仍是两段
        assertEquals(2, service.selectActiveTrace(card.getId()).size());
    }

    @Test
    void rollbackBuriesLayerOldAndNewNeverSideBySide() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        service.advance(card.getId(), "第一回整改", "enterprise");
        service.advance(card.getId(), "第一回复查不合格", "inspector");
        assertEquals(3, service.selectActiveTrace(card.getId()).size());

        // 复查不过关：往回收一格，退出的复查段在屏层压底，卡落回整改段；企业原整改报送保留
        TPostRectifyCard back = service.rollback(card.getId(), "inspector");
        assertEquals(1, back.getStage().intValue());
        assertNull(back.getReviewResult());
        assertEquals("第一回整改", back.getContent());

        List<TPostRectifyCardRecord> active = service.selectActiveTrace(card.getId());
        // 在屏为下达、整改两段；复查段旧层已压底，绝无两层同屏并列
        assertEquals(2, active.size());
        assertEquals(0, active.stream().filter(x -> x.getStage() == 2).count());
        assertEquals(1, active.stream().filter(x -> x.getStage() == 1).count());

        // 压底旧层：名目一字不改，active_slot 为空
        List<TPostRectifyCardRecord> all = service.selectAllRecords(card.getId());
        TPostRectifyCardRecord buriedReview = all.stream()
                .filter(x -> x.getStage() == 2 && x.getLayerNo() == 0).findFirst().get();
        assertEquals(1, buriedReview.getBuried().intValue());
        assertNull(buriedReview.getActiveSlot());
        assertTrue(buriedReview.getContent().contains("第一回复查不合格"));

        // 复查改口重报：再往前一格，复查段另开新层（layer_no=1），旧层依旧压底
        TPostRectifyCard again = service.advance(card.getId(), "第二回复查合格", "inspector");
        assertEquals(2, again.getStage().intValue());

        all = service.selectAllRecords(card.getId());
        assertEquals(2, all.stream().filter(x -> x.getStage() == 2).count());
        TPostRectifyCardRecord newReview = all.stream()
                .filter(x -> x.getStage() == 2 && x.getLayerNo() == 1).findFirst().get();
        assertEquals(1, newReview.getActiveSlot().intValue());
        assertTrue(newReview.getContent().contains("第二回复查合格"));
        // 底下一层与后补的那一层不同屏并列：在屏主线复查段仍只一层
        active = service.selectActiveTrace(card.getId());
        assertEquals(3, active.size());
        assertEquals(1, active.stream().filter(x -> x.getStage() == 2).count());
    }

    @Test
    void rollbackRejectedAtIssueStage() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        assertNull(service.rollback(card.getId(), "zhang"));
    }

    @Test
    void displayedStageYieldsToComputedStage() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        service.advance(card.getId(), "整改", "enterprise");
        service.advance(card.getId(), "复查", "inspector");

        // 屏上写的号被人手改成 9，与按履历算出的 2 打架
        TPostRectifyCard tampered = cardMapper.selectById(card.getId());
        tampered.setStage(9);
        cardMapper.updateById(tampered);

        TPostRectifyCard reread = service.selectTPostRectifyCardById(card.getId());
        assertEquals(2, reread.getStage().intValue());
        assertEquals(2, service.selectTPostRectifyCardList(null).get(0).getStage().intValue());
    }

    @Test
    void unknownActionRejected() {
        TPostRectifyCard card = openCard(new Date(System.currentTimeMillis() + 86400000L));
        assertNull(service.advance(card.getId(), "jump", "x", "y"));
    }

    private TPostRectifyCard openCard(Date deadline) {
        TPostRectifyCard card = service.open(OUTLET, "青屏县某路1号", "HC-9", deadline, "zhang");
        assertNotNull(card);
        return card;
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
