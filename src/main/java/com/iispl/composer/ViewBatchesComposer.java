package com.iispl.composer;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

import com.iispl.dto.InwardBatchDto;
import com.iispl.service.BpxfUploadService;
import com.iispl.serviceImpl.BpxfUploadServiceImpl;
import java.time.format.DateTimeFormatter;

public class ViewBatchesComposer extends SelectorComposer<Component>{
	
	private List<InwardBatchDto> allBatches   = null;
	private final BpxfUploadService service   = new BpxfUploadServiceImpl();
	 @Wire("#fileListbox")     private Listbox  fileListbox;
	 private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	 
	 @Override
	    public void doAfterCompose(Component comp) throws Exception {
	        super.doAfterCompose(comp);
	        loadBatches();
	    }
	 
	private void loadBatches() {
        allBatches = service.getAllBatches();
        fileListbox.getItems().clear();
        for (InwardBatchDto dto : allBatches) {
            appendRow(dto);
        }
    }
	
	 private void appendRow(InwardBatchDto dto) {
	        Listitem item = new Listitem();
	        item.appendChild(new Listcell(String.valueOf(fileListbox.getItemCount() + 1)));
	        item.appendChild(new Listcell(dto.getBatchId()));
	        item.appendChild(new Listcell(dto.getSourceFileName()));
	        item.appendChild(new Listcell(String.valueOf(dto.getTotalCheques())));
	        item.appendChild(new Listcell(String.valueOf(dto.getMicrErrorCount())));
	        item.appendChild(new Listcell(dto.getParsedAt() != null
	                ? dto.getParsedAt().format(FMT) : "—"));
	        item.appendChild(new Listcell(dto.getStatus()));

	        // Action button
	        Button viewBtn = new Button("View");
	        viewBtn.setSclass("btn-view");
	       
	        Listcell actionCell = new Listcell();
	        actionCell.appendChild(viewBtn);
	        item.appendChild(actionCell);
	        
	        viewBtn.addEventListener("onClick", event -> {
	            Executions.sendRedirect("/inward/viewBatches/viewBatches.zul");
	        });
	        
	        Button repairBtn = new Button("Repair");
	        repairBtn.setSclass("btn-view");
	        actionCell.appendChild(repairBtn);
	        item.appendChild(actionCell);
	        
	        repairBtn.addEventListener("onClick", event -> {
	            Executions.sendRedirect("/inward/inwardMicr/RejectRepair.zul");
	        });

	        fileListbox.appendChild(item);
	    }
}
