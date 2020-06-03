/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;

/**
 * Runnable thread for updating AsyncQueryResult.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncQueryUpdateThread implements Runnable {

    private Elide elide;
    private Future<AsyncQueryResult> task;
    private AsyncQuery queryObj;
    private AsyncQueryDAO asyncQueryDao;

    @Override
    public void run() {
        updateQuery();
    }

    /**
     * This is the main method which interrupts the Async Query request, if it has executed beyond
     * the maximum run time.
     */
    protected void updateQuery() {
        try {
            AsyncQueryResult queryResultObj = task.get();
            // add queryResult object to query object
            asyncQueryDao.updateAsyncQueryResult(queryResultObj, queryObj.getId());
            // If we receive a response update Query Status to complete
            asyncQueryDao.updateStatus(queryObj.getId(), QueryStatus.COMPLETE);

        } catch (InterruptedException e) {
            // Incase the future.get is interrupted , the underlying query may still have succeeded
            log.error("InterruptedException: {}", e);
            asyncQueryDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            // We set the QueryStatus to FAILURE
            // No AsyncQueryResult will be set for this case
            asyncQueryDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE);

        }
    }
}
