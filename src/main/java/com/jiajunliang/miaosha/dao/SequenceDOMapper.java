package com.jiajunliang.miaosha.dao;

import com.jiajunliang.miaosha.dataobject.SequenceDO;

public interface SequenceDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    int deleteByPrimaryKey(String name);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    int insert(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    int insertSelective(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    SequenceDO selectByPrimaryKey(String name);

    SequenceDO getSequenceByName(String name);


    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    int updateByPrimaryKeySelective(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Sat Feb 06 17:37:52 GMT 2021
     */
    int updateByPrimaryKey(SequenceDO record);

}