package Main.Home.Logic;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import Main.*;
import Main.Authentication.Logic.FileManager;
import Main.Authentication.Model.AccountType;
import Main.Authentication.Model.User;
import Main.Authentication.Model.UserAdapter;
import Main.Command.NavigationInvoker;
import Main.Command.Previous;
import Main.InventoryHelper.IAdapter;


import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/*
    Class implements manage customers page
 */
public class ManageCustomers implements IAdapter {
    public TableColumn cidCol;
    public TableColumn cUsernameCol;
    public TableColumn cNameCol;
    public TableColumn cCreatedCol;
    public TableView cTable;
    public FileManager io = new FileManager();
    SimpleDateFormat fromUser = new SimpleDateFormat("dd/MM/yyyy hh:mm");
    SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");


    int rowName;

    @Override
    public void init() throws ParseException {
        List<UserAdapter> customers=new ArrayList<>();
        for(User u : Statics.Users.stream().filter(user -> user.getType() == AccountType.CUSTOMER).collect(Collectors.toList())) {
            customers.add(new UserAdapter(u));
        }
        final ObservableList<UserAdapter> data = FXCollections.observableList(customers);

        cidCol.setCellValueFactory(new PropertyValueFactory<UserAdapter, String>("id"));
        cUsernameCol.setCellValueFactory(new PropertyValueFactory<UserAdapter, String>("username"));
        cNameCol.setCellValueFactory(new PropertyValueFactory<UserAdapter, String>("name"));


        cTable.setItems(data);
        cTable.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue)-> {
            if(cTable.getSelectionModel().getSelectedItem() != null){
                TableView.TableViewSelectionModel Tv = cTable.getSelectionModel();
                ObservableList cells = Tv.getSelectedCells();
                TablePosition tp = (TablePosition) cells.get(0);
                Object val = tp.getTableColumn().getCellData(newValue);

                for(int i = 0; i < Statics.Users.size(); i++){
                    if(validateUser(val,i)){
                        rowName = i;
                    }
                }
            }
        });

        Main.currentStage.setOnKeyListener(e-> {
            class BackSpace implements IMethod {

                @Override
                public void execute() {
                    if(AlertBox.DISPLAY_QUESTION_ANSWER)
                    if(e.getCode() == KeyCode.BACK_SPACE || e.getCode() == KeyCode.X){
                        Statics.Users.remove(rowName);
                        io.serializeToFile("CustomerDB.ser", Statics.Users.stream().filter((user)->user.getType() == AccountType.CUSTOMER).collect(Collectors.toList()));
                        new NavigationInvoker(new Previous(Main.currentStage)).activate();
                    }
                }
            }

            AlertBox.displayQuestion("Delete", "Are you sure you want to delete this user?", "Delete", "Keep",new BackSpace());


        });
    }

    private boolean validateUser(Object val,int i){
        return Statics.Users.get(i).getId().equals(val)
                || Statics.Users.get(i).getUsername().equals(val)
                || Statics.Users.get(i).getName().equals(val)
                || new SimpleDateFormat("dd/MM/yyyy hh:mm").format(Long.parseLong(Statics.Users.get(i).getId())).equals(val);

    }

    @Override
    public void custom(Object... args) {
    }

    public void cancelChanges(ActionEvent actionEvent) throws IOException {
        new NavigationInvoker(new Previous(Main.currentStage)).activate();
    }

    public void saveChanges(ActionEvent actionEvent) throws IOException {
        new NavigationInvoker(new Previous(Main.currentStage)).activate();
    }

    public void addUser(ActionEvent actionEvent) throws IOException, ParseException {
        Main.currentStage.setFXMLScene("Home/UI/addUser.fxml", new AddUserController());
    }
}
