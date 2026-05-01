package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 凭证提取链接明细数据访问接口。
 * 用于查看一个提取链接覆盖的凭证明细。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialExtractLinkItemMapper {

    @Select("SELECT i.id, i.batch_id AS batchId, i.category_id AS categoryId, i.item_no AS itemNo, i.secret_mask AS secretMask, "
            + "i.checksum, i.source_type AS sourceType, i.status, i.consumed_user_id AS consumedUserId, "
            + "DATE_FORMAT(i.consumed_at, '%Y-%m-%d %H:%i:%s') AS consumedAt, i.consume_biz_no AS consumeBizNo, "
            + "DATE_FORMAT(i.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(i.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM credential_extract_link_item li INNER JOIN credential_item i ON i.id = li.item_id "
            + "WHERE li.link_id = #{linkId} ORDER BY li.sort_no ASC, li.id ASC")
    List<CredentialItemEntity> findItemsByLinkId(@Param("linkId") Long linkId);

    @Select("SELECT item_id FROM credential_extract_link_item WHERE link_id = #{linkId} ORDER BY sort_no ASC, id ASC")
    List<Long> findItemIdsByLinkId(@Param("linkId") Long linkId);

    @Insert("INSERT INTO credential_extract_link_item(link_id, item_id, batch_id, sort_no) VALUES(#{linkId}, #{itemId}, #{batchId}, #{sortNo})")
    int insert(@Param("linkId") Long linkId,
               @Param("itemId") Long itemId,
               @Param("batchId") Long batchId,
               @Param("sortNo") int sortNo);
}
