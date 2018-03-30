/**
 * 
 */
package fr.toutatice.ecm.platform.automation.transaction;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import fr.toutatice.ecm.platform.automation.transaction.io.PreMessageBodyWriter;
import fr.toutatice.ecm.platform.automation.transaction.operation.CommitOrRollbackTransaction;
import fr.toutatice.ecm.platform.automation.transaction.operation.MarkTransactionAsRollback;


/**
 * @author david
 *
 */
public class TransactionalConversation implements Callable<Object> {

    private static final Log log = LogFactory.getLog(TransactionalConversation.class);

    public static final String NOT_FILLED = "Not_filled";

    private ExecutorService executor;
    
    private Principal principal;
    private String repositoryName;

    private Transaction tx;
    private CoreSession session;

    private AutomationService opSrv;

    private OperationContext opCtx;
    private String opId;
    private Map<String, Object> params;

    private String txcId;

    private boolean start = true;

    public TransactionalConversation(Principal principal, String repositoryName, ExecutorService executor) {
        super();

        this.principal = principal;
        this.repositoryName = repositoryName;
        this.executor = executor;

        // Initial value: null
        this.opSrv = Framework.getService(AutomationService.class);
    }

    @Override
    public Object call() throws Exception {
        Object result = null;
            if (start) 
            {
                open(this.principal, this.repositoryName);
                this.start = false;
            } else
            {
                if (StringUtils.equals(CommitOrRollbackTransaction.ID, opId)) 
                {
                    this.saveNClose(); 
                } else
                {
                    try {
                        if (StringUtils.isNotBlank(opId)) {

                            OperationContext ctx = new OperationContext(this.session);
                            ctx.setInput(opCtx.getInput());
                            ctx.setCommit(false);

                            if (log.isDebugEnabled()) {
                                log.debug("Executing in transaction " + this.getTxcId());
                            }
                            if (!StringUtils.equals(MarkTransactionAsRollback.ID, opId))
                            {
                                result = this.opSrv.run(ctx, opId, params);

                                PreMessageBodyWriter.prepareResult(result);
                            }
                            else
                            {
                                TransactionHelper.setTransactionRollbackOnly();
                            }
                        }

                    } catch (Exception e) {
                        log.error("Exception in transaction :" + e);
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
            return result;
    }

    public void open(Principal principal, String repositoryName) throws SystemException {
        open(principal, repositoryName, -1);
    }

    public void open(Principal principal, String repositoryName, int txTimeout) throws SystemException {
        // Tx
        if (txTimeout == -1) {
            TransactionHelper.startTransaction();
        } else {
            TransactionHelper.startTransaction(txTimeout);
        }
        this.tx = NuxeoContainer.getTransactionManager().getTransaction();

        // Session
        this.session = CoreInstance.openCoreSession(repositoryName, principal);
    }

    public void saveNClose() {
        if (this.session != null) {
            // Persist
            if (log.isDebugEnabled()) {
                log.debug("Saving session");
            }

            this.session.save();

            if (log.isDebugEnabled()) {
                log.debug("Session saved");
            }

            if (log.isDebugEnabled()) {
                log.debug("Closing session");
            }

            this.session.close();

            if (log.isDebugEnabled()) {
                log.debug("Session closed");
            }
        }

        if (this.tx != null) {

            if (log.isDebugEnabled()) {
                log.debug("Commiting or rollbacking transaction");
            }

            TransactionHelper.commitOrRollbackTransaction();

            if (log.isDebugEnabled()) {
                log.debug("Transaction commit or rollback");
            }
        }
    }

    public synchronized void setOperationId(String opId) {
        this.opId = opId;
    }

    public synchronized void setOperationContext(OperationContext operationCtx) {
        this.opCtx = operationCtx;
    }

    public synchronized void setParams(Map<String, Object> params) {
        this.params = params;
    }

    /**
     * Getter for txcId.
     * @return the txcId
     */
    public String getTxcId() {
        return txcId;
    }


    /**
     * Setter for txcId.
     * @param txcId the txcId to set
     */
    public void setTxcId(String txcId) {
        this.txcId = txcId;
    }

    
    /**
     * Getter for executor.
     * @return the executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

}
