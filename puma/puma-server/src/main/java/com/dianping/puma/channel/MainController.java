package com.dianping.puma.channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.dianping.puma.ComponentContainer;
import com.dianping.puma.common.SystemStatusContainer;
import com.dianping.puma.core.codec.EventCodec;
import com.dianping.puma.core.codec.EventCodecFactory;
import com.dianping.puma.core.event.ChangedEvent;
import com.dianping.puma.core.util.ByteArrayUtils;
import com.dianping.puma.filter.EventFilterChain;
import com.dianping.puma.filter.EventFilterChainFactory;
import com.dianping.puma.storage.BufferedEventChannel;
import com.dianping.puma.storage.EventChannel;
import com.dianping.puma.storage.EventStorage;
import com.dianping.puma.storage.exception.StorageException;

/**
 * @author wukezhu
 */
@Controller
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    /**
     * 首页，使用说明
     * 
     * @param clientName
     * @param seq
     * @param target
     * @param isDml
     * @param isDdl
     * @param isNeedsTransactionMeta
     * @param databaseTables
     * @param codecType
     * @param binlog
     * @param timestamp
     * @param serverId
     * @param binlogPos
     * @throws IOException
     */
    @RequestMapping(value = { "/channel" })
    public ModelAndView acceptor(HttpServletResponse res, @RequestParam("name") String clientName, Long seq,
                                 String target, @RequestParam("dml") boolean isDml, @RequestParam("ddl") boolean isDdl,
                                 @RequestParam("ts") boolean isNeedsTransactionMeta,
                                 @RequestParam("dt") String[] databaseTables, @RequestParam("codec") String codecType,
                                 String binlog, @RequestParam(value = "timestamp", required = false) Long timestamp,
                                 Long serverId, Long binlogPos) throws IOException {
        LOG.info("Client(" + clientName + ") connected.");
        // status report
        SystemStatusContainer.instance.addClientStatus(clientName, seq, target, isDml, isDdl, isNeedsTransactionMeta,
                                                       databaseTables, codecType);

        EventCodec codec = EventCodecFactory.createCodec(codecType);
        EventFilterChain filterChain = EventFilterChainFactory.createEventFilterChain(isDdl, isDml,
                                                                                      isNeedsTransactionMeta,
                                                                                      databaseTables);

        res.setContentType("application/octet-stream");
        res.addHeader("Connection", "Keep-Alive");

        String binlogFile = binlog;
        EventStorage storage = ComponentContainer.SPRING.lookup("storage-" + target, EventStorage.class);
        EventChannel channel;

        try {

            try {
                timestamp = (timestamp == null) ? -1 : timestamp;
                channel = new BufferedEventChannel(storage.getChannel(seq, serverId, binlogFile, binlogPos, timestamp),
                                                   5000);
            } catch (StorageException e1) {
                LOG.error(e1.getMessage(), e1);
                throw new IOException(e1);
            }

            while (true) {
                try {
                    filterChain.reset();
                    ChangedEvent event = channel.next();
                    if (event != null) {
                        if (filterChain.doNext(event)) {
                            byte[] data = codec.encode(event);
                            res.getOutputStream().write(ByteArrayUtils.intToByteArray(data.length));
                            res.getOutputStream().write(data);
                            res.getOutputStream().flush();
                            // status report
                            SystemStatusContainer.instance.updateClientSeq(clientName, event.getSeq());
                        }
                    }
                } catch (Exception e) {
                    SystemStatusContainer.instance.removeClient(clientName);
                    LOG.info("Client(" + clientName + ") failed. ", e);
                    break;
                }
            }

            channel.close();

        } finally {
            SystemStatusContainer.instance.removeClient(clientName);

        }

        LOG.info("Client(" + clientName + ") disconnected.");

        return null;
    }

    @RequestMapping(value = "/status")
    public ModelAndView stat() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("systemStatus", SystemStatusContainer.instance);

        return new ModelAndView("status", map);
    }

}
