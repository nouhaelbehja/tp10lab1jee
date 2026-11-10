package com.example.util;
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
public class PerformanceReport {
    private final Statistics stats;
    public PerformanceReport(EntityManagerFactory emf) { stats = emf.unwrap(SessionFactory.class).getStatistics(); }
    public void printReport() {
        System.out.println("=== RAPPORT DE PERFORMANCE ===");
        System.out.println("Requetes JPQL/Criteria : " + stats.getQueryExecutionCount());
        System.out.println("Statements JDBC prepares : " + stats.getPrepareStatementCount());
        System.out.println("Cache L2 hits : " + stats.getSecondLevelCacheHitCount());
        System.out.println("Cache L2 misses : " + stats.getSecondLevelCacheMissCount());
        System.out.println("Cache L2 puts : " + stats.getSecondLevelCachePutCount());
        System.out.println("Cache requetes hits : " + stats.getQueryCacheHitCount());
        System.out.println("Transactions : " + stats.getTransactionCount());
        long total = java.util.Arrays.stream(stats.getQueries())
            .mapToLong(query -> stats.getQueryStatistics(query).getExecutionTotalTime()).sum();
        System.out.println("Duree cumulee requetes suivies (ms) : " + total);
    }
}
