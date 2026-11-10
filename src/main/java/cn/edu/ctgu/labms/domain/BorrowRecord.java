package cn.edu.ctgu.labms.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 设备借用与归还记录实体
 */
public class BorrowRecord {
    private final String recordId;
    private final String equipmentCode;
    private final String borrowerId;
    private final String purpose;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private Boolean returnConditionOk;
    private final String approvedBy;

    public BorrowRecord(String recordId, String equipmentCode, String borrowerId,
                        String purpose, LocalDate borrowDate, LocalDate dueDate,
                        LocalDate returnDate, Boolean returnConditionOk, String approvedBy) {
        this.recordId = Objects.requireNonNull(recordId, "recordId cannot be null");
        this.equipmentCode = Objects.requireNonNull(equipmentCode, "equipmentCode cannot be null");
        this.borrowerId = Objects.requireNonNull(borrowerId, "borrowerId cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose cannot be null");
        this.borrowDate = Objects.requireNonNull(borrowDate, "borrowDate cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        if (dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("应还日期不能早于借出日期");
        }
        this.returnDate = returnDate;
        this.returnConditionOk = returnConditionOk;
        this.approvedBy = Objects.requireNonNull(approvedBy, "approvedBy cannot be null");
    }

    public String getRecordId() {
        return recordId;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public Boolean getReturnConditionOk() {
        return returnConditionOk;
    }

    public void setReturnConditionOk(Boolean returnConditionOk) {
        this.returnConditionOk = returnConditionOk;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOverdue(LocalDate currentDate) {
        LocalDate checkDate = (returnDate != null) ? returnDate : currentDate;
        return checkDate.isAfter(dueDate);
    }

    @Override
    public String toString() {
        String state = isReturned()
                ? (Boolean.TRUE.equals(returnConditionOk) ? "已归还(完好)" : "已归还(受损转维修)")
                : "借出中";
        return String.format("BorrowRecord{id='%s', eq='%s', borrower='%s', borrowDate=%s, dueDate=%s, state='%s'}",
                recordId, equipmentCode, borrowerId, borrowDate, dueDate, state);
    }
}
