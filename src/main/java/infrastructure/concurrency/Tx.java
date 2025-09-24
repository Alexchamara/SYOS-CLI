package infrastructure.concurrency;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

public final class Tx {
    private final DataSource ds;
    public Tx(DataSource ds) { this.ds = ds; }

    public <T> T inTx(Function<Connection, T> work) {
        try (var con = ds.getConnection()) {
            boolean old = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                T result = work.apply(con);
                con.commit();
                con.setAutoCommit(old);
                return result;
            } catch (RuntimeException e) {
                try {
                    con.rollback();
                } catch (Exception rollbackEx) {
                    // If rollback fails, throw rollback exception
                    throw new RuntimeException(rollbackEx);
                }
                con.setAutoCommit(old);
                throw new RuntimeException(e.getMessage(), e);
            } catch (Exception e) {
                try {
                    con.rollback();
                } catch (Exception rollbackEx) {
                    // Preserve original exception message if rollback fails
                    throw new RuntimeException(e.getMessage(), e);
                }
                con.setAutoCommit(old);
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw e; // Re-throw RuntimeException as is
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}