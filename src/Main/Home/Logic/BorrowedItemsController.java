package Main.Home.Logic;

import Main.Authentication.Model.AccountType;
import Main.Interceptor.PostLoginContext;
import Main.Momento.Caretaker;
import Main.Momento.Originator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import Main.AlertBox;
import Main.Authentication.Logic.FileManager;
import Main.Authentication.Logic.LoginController;
import Main.Authentication.Model.User;
import Main.Command.NavigationInvoker;
import Main.Command.Previous;
import Main.Home.Model.Machine;
import Main.Home.Model.MachineAdapter;
import Main.IMethod;
import Main.Main;
import Main.InventoryHelper.IAdapter;
import Main.Statics;

import javax.crypto.Mac;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static Main.Main.myDispatcher;

/*
    Class implements logic of borrowed items page.
 */
public class BorrowedItemsController implements IAdapter {

    public TableColumn machineCol;
    public TableColumn dueCol;
    public TableView<MachineAdapter> returnView;
    public TableColumn idCol;
    public TableColumn nameCol;
    public TableColumn typeCol;
    public TableColumn costPerDayCol;
    public TableColumn invCol;
    public Button cancelBtn;
    public Button undoBorrow;
    public Button redoBorrow;
    private FileManager io = new FileManager();
    List<MachineAdapter> machines = new ArrayList<>();
    ArrayList<User> users = new ArrayList<>();
    public Label unameField;

    static Caretaker<Map<String, Object>> caretaker = new Caretaker<>();
    static Originator<Map<String, Object>> originator = new Originator<> ();
    static int currentStateIndex,savedStateCounter = 0;

    @Override
    public void init() {
        if (Statics.CurrentUser != null) {

            unameField.setText(Statics.CurrentUser.getUsername() + "(" + Statics.CurrentUser.getType().name().toLowerCase() + ")");

            Arrays.asList("CustomerDB.ser").forEach(path-> {
                try {
                    io.readSerializedFile(path,"users");
                   users.addAll(io.users);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

          cancelBtn.setOnAction(e->{
              new NavigationInvoker(new Previous(Main.currentStage)).activate();
          });

            System.out.println(Statics.CurrentUser);
            machines.clear();
            for (Machine u : Statics.CurrentUser.getCurrRentals().stream().collect(Collectors.toList())) {
                System.out.println("Line 70");
                System.out.println(u.getInventory());
                machines.add(new MachineAdapter(u));
            }

            final ObservableList<MachineAdapter> data = FXCollections.observableList(machines);
            idCol.setCellValueFactory(new PropertyValueFactory<MachineAdapter, String>("id"));
            nameCol.setCellValueFactory(new PropertyValueFactory<MachineAdapter, String>("name"));
            typeCol.setCellValueFactory(new PropertyValueFactory<MachineAdapter, String>("type"));
            costPerDayCol.setCellValueFactory(new PropertyValueFactory<MachineAdapter, String>("costPerDay"));
            invCol.setCellValueFactory(new PropertyValueFactory<MachineAdapter, String>("inventory"));

            myDispatcher.onPostInventory(new PostLoginContext(Statics.CurrentUser.getUsername()));

            returnView.setItems(data);

        }

    }

    public void loadUsers() {
        Arrays.asList("AdminDB.ser", "CustomerDB.ser").forEach(path -> {
            try {
                System.out.println(path);
                io.readSerializedFile(path, "users");
                Statics.Users.addAll(io.users);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void custom(Object... args) {
    }

    /*
        clicking settings icon brings back to login
     */
    public void onSettings(MouseEvent mouseEvent) {
        ArrayList<User> users= new ArrayList<>();
        new FileManager().serializeToFile("currentUser.ser",users);
        try {
            Main.currentStage.setFXMLScene("Authentication/UI/login.fxml",new LoginController());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    int rowName=0;
    String selectedRow="";
    public void onReturn(ActionEvent actionEvent) {
        selectedRow= returnView.getSelectionModel().getSelectedItem().getName();
        class Borrow implements IMethod {

            @Override
            public void execute() {
                if (AlertBox.DISPLAY_QUESTION_ANSWER) {
                    MachineAdapter mac = returnView.getSelectionModel().getSelectedItem();


                    String selectedName = mac.getName();
                    selectedRow=selectedName;
                    System.out.println("---------");
                    System.out.println(mac.getName());
                    int index=-1;
                    for(int i=0; i< Statics.Machines.size();i++){
                        if(mac.getId().equals(Statics.Machines.get(i).getId()))index=i;
                    }

                    //check if valid
                    int quantity=1;
                    if(AlertBox.DISPLAY_INPUT_TEXT.matches("[0-9]+")){
                        quantity=Integer.parseInt(AlertBox.DISPLAY_INPUT_TEXT);
                        System.out.println((mac.getInventory()));
                        quantity=Math.min(mac.getInventory(),quantity);
                        System.out.println("I am returning "+ quantity + ": " +selectedName);
                        System.out.println((mac.getInventory()-quantity+" Left!!"));

                        Machine m =mac.getMachine();
                        Statics.CurrentUser.getCurrRentals().stream().filter(machine -> machine.getId().equals(m.getId())).collect(Collectors.toList()).get(0).decreaseInventory(quantity);
                        System.out.println(Statics.CurrentUser.getCurrRentals().stream().filter(machine -> machine.getId().equals(m.getId())).collect(Collectors.toList()).get(0).getInventory());
                         Statics.Machines.stream().filter(machine -> machine.getId().equals(mac.getId())).collect(Collectors.toList()).get(0).increaseInventory(quantity);
                       // Statics.Machines.get(index).setInventory(Math.max((Statics.CurrentUser.getCurrRentals().get(rowName).getInventory()-quantity), 0));
                        AlertBox.display("SUCCESS", String.format("%s\n%-15s:\t%-15s","Thank You, Product Returned",selectedName,quantity ));
                        System.out.println(Statics.CurrentUser.getCurrRentals().stream().filter(machine -> machine.getId().equals(m.getId())).collect(Collectors.toList()).get(0).getInventory());

                        System.out.println("MOMENTO STRTED");
                        Map<String, Object> state = new HashMap<>();
                        state.put("machine",m);
                        state.put("quantity",quantity);
                        //store this machine in momento;
                        originator.setState(state);
                        caretaker.refresh(currentStateIndex);
                        caretaker.saveState(originator.storeState());
                        savedStateCounter++;
                        currentStateIndex++;

                        //0,1,2,3


                        undoBorrow.setDisable(false);

                        System.out.println("MOMENTO STORED: Quantity -- "+quantity);
                        //end of momento

                        System.out.println(Statics.CurrentUser.getCurrRentals().stream().filter(machine -> machine.getId().equals(m.getId())).collect(Collectors.toList()).get(0).getInventory());

                        //save machines
                        io.machineSerializeToFile("MachineDB.ser", Statics.Machines);
                        //save user;
                        ArrayList<User> u = new ArrayList<User>();
                        u.add(Statics.CurrentUser);
                        io.serializeToFile("currentUser.ser",u);
                        io.serializeToFile("CustomerDB.ser",  Statics.Users.stream().filter((user)->user.getType() == AccountType.CUSTOMER).collect(Collectors.toList()));
                       // new NavigationInvoker(new Previous(Main.currentStage)).activate();
                        try {
                            init();
                         //   Main.currentStage.setFXMLScene("Home/UI/borrowedItems.fxml", new BorrowedItemsController());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }


                }
            }
        }

        AlertBox.displayInput("Return "+selectedRow, "How many "+selectedRow+" Would you like to return?",
                "Confirm", "Cancel","Enter how much you want to return",new Borrow());

    }
    /*
        goback to bring back to userHome UI
     */
    public void goBack(ActionEvent actionEvent) {
        new NavigationInvoker(new Previous(Main.currentStage)).activate();
    }

    public void doMomento(boolean undo){
        Map oldState = originator.retrieveState(caretaker.getState(currentStateIndex));
        Machine oldMachine = (Machine) oldState.get("machine");
        int quantity = (int) oldState.get("quantity");
        System.out.println("CURRENT STATE V SAVED STATE : "+ currentStateIndex+" :: "+savedStateCounter);
        int position = -1;
        for(int i = 0 ; i <  Statics.CurrentUser.getCurrRentals().size();i++){
            if( Statics.CurrentUser.getCurrRentals().get(i).getId().equals(oldMachine.getId()))
            {
                position = i;
                break;
            }

        }

        if(position>=0){
            if(undo)
            {
                oldMachine.increaseInventory(quantity);
                Statics.CurrentUser.getCurrRentals().set(position,oldMachine);
                Statics.Machines.stream().filter(machine -> machine.getId().equals(oldMachine.getId())).collect(Collectors.toList()).set(0,oldMachine);//.decreaseInventory(quantity);
            }
            else{
                System.out.println("REDOING>>>>");
                oldMachine.decreaseInventory(quantity);
                Statics.CurrentUser.getCurrRentals().set(position,oldMachine);
                Statics.Machines.stream().filter(machine -> machine.getId().equals(oldMachine.getId())).collect(Collectors.toList()).set(0,oldMachine);
            }

        }
        //save machines
        io.machineSerializeToFile("MachineDB.ser", Statics.Machines);
        //save user;
        ArrayList<User> u = new ArrayList<User>();
        u.add(Statics.CurrentUser);
        io.serializeToFile("currentUser.ser",u);
        io.serializeToFile("CustomerDB.ser",  Statics.Users.stream().filter((user)->user.getType() == AccountType.CUSTOMER).collect(Collectors.toList()));

    }
    public void onUndoBorrow(ActionEvent actionEvent) {
        System.out.println("Undo Pressed");
        if(currentStateIndex >= 1){
            currentStateIndex--;
            //restore old machine state
            System.out.println("Undo started");
            doMomento(true);
            if(currentStateIndex<=0)
                undoBorrow.setDisable(true);
            redoBorrow.setDisable(false);
            init();
        }
    }

    public void onRedoBorrow(ActionEvent actionEvent) {

        System.out.println(">"+(savedStateCounter ) +">"+ currentStateIndex);
        System.out.println((savedStateCounter ) > currentStateIndex);
        if((savedStateCounter) >= currentStateIndex){

            System.out.println("Redo started");
            doMomento(false);
            currentStateIndex++;
            if(currentStateIndex>=savedStateCounter)
                redoBorrow.setDisable(true);

            undoBorrow.setDisable(false);


            init();
        }else if((currentStateIndex) == 0){

            System.out.println("Redo started");
            doMomento(false);
            undoBorrow.setDisable(false);

            currentStateIndex++;
            init();
        }
        else{
            redoBorrow.setDisable(true);
        }
    }
}
