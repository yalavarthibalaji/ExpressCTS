package com.iispl.service;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;

import com.iispl.daoImpl.UserDaoImpl;
import com.iispl.dto.LoginDTO;
import com.iispl.jaxb.BpxfCheque;
import com.iispl.jaxb.BpxfRoot;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;
import com.iispl.serviceImpl.NotificationServiceImpl;
import com.iispl.util.BpxfParser;

public class BpxfFolderWatchService implements Runnable {

    private static final Logger LOG = Logger.getLogger(BpxfFolderWatchService.class.getName());

    public static final String WATCH_FOLDER = "/home/administrator/BpxfFile";
    public static final String QUEUE_NAME   = "bpxfWatchQueue";

    private final LoginDTO              operator;
    private final BpxfUploadServiceImpl service = new BpxfUploadServiceImpl();
    private volatile boolean            running  = true;

    public BpxfFolderWatchService(LoginDTO operator) {
        this.operator = operator;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        try {
            Path folder = Paths.get(WATCH_FOLDER);
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
                LOG.info("Created watch folder: " + WATCH_FOLDER);
            }

            WatchService watcher = FileSystems.getDefault().newWatchService();
            folder.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            LOG.info("Watching folder: " + WATCH_FOLDER);

            while (running) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path   filePath = folder.resolve((Path) event.context());
                        String fileName = filePath.getFileName().toString();

                        if (!fileName.endsWith(".zip")) continue;

                        Thread.sleep(500);
                        LOG.info("New ZIP detected: " + fileName);
                        processFile(filePath, fileName);
                    }
                }

                key.reset();
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "WatchService error", e);
        }
    }

    private void processFile(Path zipPath, String fileName) {
        try {
            String batchId = fileName.replace(".zip", "");

            BpxfParser.ParseResult result = BpxfParser.parse(zipPath.toFile(), batchId);
            BpxfRoot root = result.getBpxfRoot();

            if (root.getCheques() != null) {
                for (BpxfCheque bpxf : root.getCheques()) {
                    bpxf.setFrontImagePath(
                            BpxfParser.buildImagePath(batchId, bpxf.getFrontImagePath()));
                    bpxf.setBackImagePath(
                            BpxfParser.buildImagePath(batchId, bpxf.getBackImagePath()));
                }
            }

            service.parseAndSaveRoot(root, fileName, operator);
            LOG.info("Parsed and saved: " + fileName);

            // ── Notify all MAKER_INWARD users ──────────────────────────
            notifyMakerInwardUsers(batchId, root.getCheques() != null
                    ? root.getCheques().size() : 0);
            // ───────────────────────────────────────────────────────────

            EventQueue<Event> queue = EventQueues.lookup(
                    QUEUE_NAME, EventQueues.APPLICATION, true);
            queue.publish(new Event(QUEUE_NAME, null, fileName));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process ZIP: " + fileName, e);
        }
    }

    private void notifyMakerInwardUsers(String batchId, int chequeCount) {
        try {
            UserDaoImpl userDao = new UserDaoImpl();
            NotificationServiceImpl notifService = new NotificationServiceImpl();

            List<Long> userIds = userDao.findUserIdsByRole("MAKER_INWARD");
            String message = "Batch " + batchId + " has been auto-imported and parsed ("
                    + chequeCount + " cheques). Ready for processing.";

            for (Long uid : userIds) {
                notifService.createNotification(uid, message);
            }
            LOG.info("Notifications sent to " + userIds.size() + " MAKER_INWARD user(s)");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to send notifications", e);
        }
    }
}