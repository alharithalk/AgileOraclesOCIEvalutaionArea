package com.agileoracles.leave_portal_app.repository;



import com.agileoracles.leave_portal_app.model.LeaveUploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface LeaveUploadRepository extends JpaRepository<LeaveUploadRecord, Long> {
    List<LeaveUploadRecord> findByUserEmail(String userEmail);
}