package com.eventtracker.repository;

import com.eventtracker.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySponsorIdOrderByPaymentDateDescIdDesc(Long sponsorId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.sponsor.id = :sponsorId")
    BigDecimal totalForSponsor(@Param("sponsorId") Long sponsorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Payment p where p.sponsor.id = :sponsorId")
    int deleteBySponsorId(@Param("sponsorId") Long sponsorId);

    /** Payment totals grouped by method: each row is [PaymentMethod, BigDecimal sum, Long count]. */
    @Query("select p.method, sum(p.amount), count(p) from Payment p group by p.method")
    List<Object[]> totalsByMethod();
}
