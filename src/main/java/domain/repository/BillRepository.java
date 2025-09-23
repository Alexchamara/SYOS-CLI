package domain.repository;

import domain.billing.Bill;

import java.sql.Connection;

public interface BillRepository {
    long save(Connection con, Bill bill); // returns generated id
}