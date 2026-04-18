package com.ai.edu.common.dto.kg;

import java.util.List;

/**
 * 同步对账结果
 */
public class ReconciliationResult {
    public final boolean matched;
    public final int mysqlTextbookCount;
    public final int neo4jTextbookCount;
    public final int mysqlChapterCount;
    public final int neo4jChapterCount;
    public final int mysqlSectionCount;
    public final int neo4jSectionCount;
    public final int mysqlKpCount;
    public final int neo4jKpCount;
    public final int mysqlTextbookChapterCount;
    public final int neo4jTextbookChapterCount;
    public final int mysqlChapterSectionCount;
    public final int neo4jChapterSectionCount;
    public final int mysqlSectionKpCount;
    public final int neo4jSectionKpCount;
    public final List<String> differences;

    public ReconciliationResult(boolean matched,
                                int mysqlTb, int neo4jTb, int mysqlCh, int neo4jCh,
                                int mysqlSec, int neo4jSec, int mysqlKp, int neo4jKp,
                                int mysqlTbCh, int neo4jTbCh, int mysqlChSec, int neo4jChSec,
                                int mysqlSecKp, int neo4jSecKp, List<String> differences) {
        this.matched = matched;
        this.mysqlTextbookCount = mysqlTb;
        this.neo4jTextbookCount = neo4jTb;
        this.mysqlChapterCount = mysqlCh;
        this.neo4jChapterCount = neo4jCh;
        this.mysqlSectionCount = mysqlSec;
        this.neo4jSectionCount = neo4jSec;
        this.mysqlKpCount = mysqlKp;
        this.neo4jKpCount = neo4jKp;
        this.mysqlTextbookChapterCount = mysqlTbCh;
        this.neo4jTextbookChapterCount = neo4jTbCh;
        this.mysqlChapterSectionCount = mysqlChSec;
        this.neo4jChapterSectionCount = neo4jChSec;
        this.mysqlSectionKpCount = mysqlSecKp;
        this.neo4jSectionKpCount = neo4jSecKp;
        this.differences = differences;
    }
}
