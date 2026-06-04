package com.iispl.service;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;

import com.iispl.dto.LoginDTO;
import com.iispl.jaxb.BpxfRoot;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class BpxfFolderWatchService implements Runnable{
	private static final Logger LOG = Logger.getLogger(BpxfFolderWatchService.class.getName());
	public static final String WATCH_FOLDER = "/home/administrator/BpxfFile";
    public static final String QUEUE_NAME   = "bpxfWatchQueue";
    
    private final LoginDTO operator;
    private final BpxfUploadServiceImpl service = new BpxfUploadServiceImpl();
    private volatile boolean running = true;
    
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
    		
    		if(!Files.exists(folder)) {
    			Files.createDirectories(folder);
    			LOG.info("Create watch folder: "+WATCH_FOLDER);
    		}
    		
    		WatchService watcher = FileSystems.getDefault().newWatchService();
    		folder.register(watcher,StandardWatchEventKinds.ENTRY_CREATE);
    		LOG.info("Watching folder : "+WATCH_FOLDER);
    		
    		while(running) {
    			WatchKey key;
    			try {
    				key = watcher.take();
    			}catch(InterruptedException e) {
    				Thread.currentThread().interrupt();
    				break;
    			}
    			
    			for(WatchEvent<?> event : key.pollEvents()) {
    				if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
    					Path filePath = folder.resolve((Path) event.context());
    					String fileName = filePath.getFileName().toString();
    					
    					if(!fileName.endsWith(".xml")) continue;
    					
    					LOG.info("New File Detected: "+fileName);
    					processFile(filePath,fileName);
    				}
    			}
    			
    			key.reset();
    		}
    	}catch(Exception e) {
    		  LOG.log(Level.SEVERE, "WatchService error", e);
    	}
    }
    
    private void processFile(Path filePath, String fileName) {
    	try(InputStream is = Files.newInputStream(filePath)){
    		JAXBContext ctx = JAXBContext.newInstance(BpxfRoot.class);
    		Unmarshaller um = ctx.createUnmarshaller();
    		BpxfRoot root = (BpxfRoot) um.unmarshal(is);
    		
    		service.parseAndSaveRoot(root, root.getHeader().getBatchId(), fileName, operator);    		
    		LOG.info("Parsed and Saved: "+fileName);
    		
    		EventQueue<Event> queue = EventQueues.lookup(QUEUE_NAME, EventQueues.APPLICATION,true);
    		queue.publish(new Event(QUEUE_NAME, null, fileName));
    	}catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process file: " + fileName, e);
        }
    }
    
}
