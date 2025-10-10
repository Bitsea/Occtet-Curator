package eu.occtet.bocfrontend.view.dialog;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@ViewController("addCopyrightDialog")
@ViewDescriptor("add-copyright-dialog.xml")
@DialogMode(width = "900px", height = "650px")
public class AddCopyrightDialog extends AbstractAddContentDialog<InventoryItem> {

    private InventoryItem inventoryItem;

    private Copyright copyright;

    @ViewComponent
    private CollectionContainer<Copyright> copyrightDc;

    @ViewComponent
    private DataGrid<Copyright> copyrightDataGrid;

    @ViewComponent
    private TextField searchField;

    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Subscribe("copyrightDataGrid")
    public void selectAvailableCopyright(final ItemClickEvent<Copyright> event){
        copyright = event.getItem();
    }

    @Override
    @Subscribe("copyrightDc")
    public void setAvailableContent(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
        copyrightDc.setItems(copyrightRepository.findAll());
    }

    @Override
    @Subscribe(id = "addCopyrightButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<Copyright> copyrights = new ArrayList<>(copyrightDataGrid.getSelectedItems());

        if(event != null & copyrights != null){
            for(Copyright copyright : copyrights){
                if(!this.inventoryItem.getCopyrights().contains(copyright)){
                    this.inventoryItem.getCopyrights().add(copyright);
                }
            }
            inventoryItemRepository.save(this.inventoryItem);
            close(StandardOutcome.CLOSE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<Copyright> list = copyrightRepository.findByCopyrightText(searchWord);
            copyrightDc.setItems(list);
        }else{
            copyrightDc.setItems(copyrightRepository.findAll());
        }
    }

    @Subscribe(id="cancelButton")
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}
}