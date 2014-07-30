/**
 * Project: puma-client File Created at 2012-8-14 $Id$ Copyright 2010 dianping.com. All rights reserved. This software
 * is the confidential and proprietary information of Dianping Company. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance with the terms of the license agreement
 * you entered into with dianping.com.
 */
package com.dianping.puma.api;

import java.util.concurrent.TimeUnit;

import com.dianping.puma.core.constant.SubscribeConstant;
import com.dianping.puma.core.event.ChangedEvent;

/**
 * @author Leo Liang
 */
public class TestApi {

    public static void main(String[] args) {
        ConfigurationBuilder configBuilder = new ConfigurationBuilder();
        configBuilder.ddl(false);
        configBuilder.dml(true);

        configBuilder.host("localhost");
        configBuilder.port(8080);
        configBuilder.target("141");

        configBuilder.serverId(1);
        configBuilder.binlog("mysql-bin.000001");
        configBuilder.binlogPos(106L);
        configBuilder.name("testClient");

        configBuilder.tables("binlog_test", "*");
        configBuilder.transaction(false);

        PumaClient pc = new PumaClient(configBuilder.build());
        pc.getSeqFileHolder().saveSeq(SubscribeConstant.SEQ_FROM_BINLOGINFO);

        pc.register(new EventListener() {

            @Override
            public void onConnectException(Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                System.out.println(">>>>>>>>>>>>>>>>>>onConnectException " + e);
            }

            @Override
            public void onSkipEvent(ChangedEvent event) {
                System.out.println(">>>>>>>>>>>>>>>>>>Skip " + event);
            }

            @Override
            public boolean onException(ChangedEvent event, Exception e) {
                System.out.println("-------------Exception " + e);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                return true;
            }

            @Override
            public void onEvent(ChangedEvent event) throws Exception {
                System.out.println(event);
                // // biz logic
                // if (event instanceof RowChangedEvent) {
                // RowChangedEvent rce = (RowChangedEvent) event;
                // if (rce.getTable().equals("TG_Order") && rce.getActionType() == RowChangedEvent.INSERT) {
                // System.out.println(rce);
                // }
                // }

            }

            @Override
            public void onConnected() {
                System.out.println(">>>>>>>>>>>>>>>>>>Connected.");
            }
        });
        pc.start();
    }
}
