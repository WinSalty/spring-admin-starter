package com.winsalty.quickstart.notice.mapper;

import com.winsalty.quickstart.notice.entity.NoticeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 公告通知数据访问接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Mapper
public interface NoticeMapper {

    String NOTICE_SELECT = "SELECT n.id, n.title, n.content, n.notice_type AS noticeType, n.priority, n.publisher_id AS publisherId, "
            + "IFNULL(u.nickname, u.username) AS publisherName, DATE_FORMAT(n.publish_time, '%Y-%m-%d %H:%i:%s') AS publishTime, "
            + "DATE_FORMAT(n.expire_time, '%Y-%m-%d %H:%i:%s') AS expireTime, n.status, n.sort_order AS sortOrder, n.deleted, "
            + "DATE_FORMAT(n.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(n.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_notice n LEFT JOIN sys_user u ON u.id = n.publisher_id ";

    @Select({
            "<script>",
            NOTICE_SELECT,
            "WHERE n.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(n.title) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(n.content) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND n.status = #{status} </if>",
            "<if test='noticeType != null and noticeType != \"\"'>AND n.notice_type = #{noticeType} </if>",
            "ORDER BY n.sort_order ASC, n.publish_time DESC, n.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<NoticeEntity> findPage(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("noticeType") String noticeType,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_notice n WHERE n.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(n.title) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(n.content) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND n.status = #{status} </if>",
            "<if test='noticeType != null and noticeType != \"\"'>AND n.notice_type = #{noticeType} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("noticeType") String noticeType);

    @Select(NOTICE_SELECT + "WHERE n.id = #{id} AND n.deleted = 0 LIMIT 1")
    NoticeEntity findById(@Param("id") Long id);

    @Select(NOTICE_SELECT + "WHERE n.deleted = 0 AND n.status = 'active' AND n.publish_time <= NOW() AND (n.expire_time IS NULL OR n.expire_time >= NOW()) ORDER BY n.sort_order ASC, n.publish_time DESC, n.id DESC")
    List<NoticeEntity> findActive();

    @Insert("INSERT INTO sys_notice(title, content, notice_type, priority, publisher_id, publish_time, expire_time, status, sort_order, deleted) VALUES(#{title}, #{content}, #{noticeType}, #{priority}, #{publisherId}, IFNULL(STR_TO_DATE(#{publishTime}, '%Y-%m-%d %H:%i:%s'), NOW()), STR_TO_DATE(#{expireTime}, '%Y-%m-%d %H:%i:%s'), #{status}, #{sortOrder}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoticeEntity entity);

    @Update("UPDATE sys_notice SET title = #{title}, content = #{content}, notice_type = #{noticeType}, priority = #{priority}, publisher_id = #{publisherId}, publish_time = IFNULL(STR_TO_DATE(#{publishTime}, '%Y-%m-%d %H:%i:%s'), publish_time), expire_time = STR_TO_DATE(#{expireTime}, '%Y-%m-%d %H:%i:%s'), status = #{status}, sort_order = #{sortOrder} WHERE id = #{id} AND deleted = 0")
    int update(NoticeEntity entity);

    @Update("UPDATE sys_notice SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
