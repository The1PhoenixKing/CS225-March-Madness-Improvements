
import java.io.*;
import java.security.*;
import java.util.*;

import javafx.application.Application;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Stage;


/**
 *  MarchMadnessGUI
 * 
 * this class contains the buttons the user interacts
 * with and controls the actions of other objects
 *
 * @author Grant Osborn
 */

public class MarchMadnessGUI extends Application {
    
    
    //all the gui elements
    private BorderPane root;
    private ToolBar toolBar;
    private ToolBar btoolBar;
    private Button saveButton;
    private Button logoutButton;
    private Button simulateButton;
    private Button scoreBoardButton;

    private Button viewPredictedBracketButton;
    private Button viewSimulatedBracketButton;
    private Button clearButton;
    private Button finalizeButton;
    private Button helpButton; //added AE
    
    //allows you to navigate back to division selection screen
    private Button back;

    // removed ArrayList<Bracket> list, HashMap<String, Bracket> playerMap
    // Katherine Foley
    
    private  Bracket startingBracket;
    //reference to currently logged in bracket
    private Bracket selectedBracket;
    private Bracket simResultBracket;



    private ScoreBoardTable scoreBoard;
    private TableView table;
    private BracketPane bracketPane;
    private GridPane loginP;
    private TournamentInfo teamInfo;



    @Override
    public void start(Stage primaryStage) {
        //try to load all the files, if there is an error display it

        // removed playerBrackets and playerMap as we are now only loading one bracket at a time
        // Katherine Foley
        try{
            teamInfo=new TournamentInfo();
            startingBracket= new Bracket(teamInfo.loadStartingBracket());
            simResultBracket=new Bracket(teamInfo.loadStartingBracket());
        } catch (IOException ex) {
            showError(new Exception(ex.getMessage(),ex),true);
        }

        //the main layout container
        root = new BorderPane();
        scoreBoard= new ScoreBoardTable();
        table=scoreBoard.start();
        loginP=createLogin();
        CreateToolBars();
        
        //display login screen
        logout();
        
        setActions();
        root.setTop(toolBar);   
        root.setBottom(btoolBar);
        Scene scene = new Scene(root);
        primaryStage.setMaximized(true);

        primaryStage.setTitle("March Madness Bracket Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * simulates the tournament  
     * simulation happens only once and
     * after the simulation no more users can login
     */

    private void simulate(){
        ArrayList<Bracket> playerBrackets;

        //cant login and restart prog after simulate
        simulateButton.setDisable(true);
        // added saveButton and viewPredictedBracketButton parameters
        // Katherine Foley
        saveButton.setDisable(true);
        viewPredictedBracketButton.setDisable(false);
        
       scoreBoardButton.setDisable(false);
       viewSimulatedBracketButton.setDisable(false);
       
       teamInfo.simulate(simResultBracket);
       playerBrackets = loadAllBrackets();
       for(Bracket b:playerBrackets){
           //redone scoring to accommodate arraylist of correct positions - Phoenix
           int score = 0;
           ArrayList<Integer> correctPositions = b.scoreBracket(simResultBracket);
           for (Integer position : correctPositions) {
               score += 32/Math.pow(2 ,(int)(Math.log10(position + 1) / Math.log10(2)));
           }
           scoreBoard.addPlayer(b, score);
       }
        
        displayPane(table);
    }
    
    /**
     * Displays the login screen
     * 
     */
    private void logout(){
        // renamed login to logout as this better encapsulates its function, added saveButton, simulateButton, and
        // viewPredictedButton
        logoutButton.setDisable(true);
        saveButton.setDisable(true);
        simulateButton.setDisable(true);
        scoreBoardButton.setDisable(true);
        viewPredictedBracketButton.setDisable(true);
        viewSimulatedBracketButton.setDisable(true);
        btoolBar.setDisable(true);
        displayPane(loginP);
    }

     /**
     * Displays the score board
     *
     */
    private void scoreBoard(){
        displayPane(table);
    }

     /**
      * Displays Simulated Bracket
      *
      */
    private void viewSimulatedBracket(){
       bracketPane=new BracketPane(simResultBracket);
       ArrayList<Integer> correctIndices = selectedBracket.scoreBracket(simResultBracket);
       bracketPane.highlightCorrect(correctIndices);
       bracketPane.switchToRegion(0);
       displayPane(bracketPane);
    }

    /**
     * Displays player picked bracket
     * Katherine Foley
     */
    private void viewPredictedBracket() {
        bracketPane=new BracketPane(selectedBracket);
        ArrayList<Integer> correctIndices = selectedBracket.scoreBracket(simResultBracket);
        bracketPane.highlightCorrect(correctIndices);
        bracketPane.switchToRegion(0);
        bracketPane.setDisable(true);
        displayPane(bracketPane);
    }
    /**
     * allows user to choose bracket
     * 
     */
   private void chooseBracket(){
        btoolBar.setDisable(false);
        bracketPane=new BracketPane(selectedBracket);
        displayPane(bracketPane);

    }

    /**
     * resets current selected sub tree
     * for final4 reset Ro2 and winner
     */
    private void clear(){

      int pos = bracketPane.clear();

      if(pos == 0) {
    	  selectedBracket=new Bracket(startingBracket);
      }
      bracketPane=new BracketPane(selectedBracket);
      bracketPane.switchToRegion(pos);
      displayPane(bracketPane);

    }

    private void help() {
        infoAlert("If you're looking for guidance on how to use this app, review the attached User Guide for an in-depth explanation, or read below:\n" +
                "\n" +
                "This app is a March Madness bracket simulator. Create a new login with a username & password of your choice, make selections for each matchup on the bracket, and run the simulation to see how well your picks stack up to the simulated bracket. The more picks you get correct, the higher your score. Create multiple users to compare scores between each user's bracket on a shared leaderboard!");
    }

    private void finalizeBracket(){
       if(bracketPane.isComplete()){
           btoolBar.setDisable(true);
           bracketPane.setDisable(true);
           simulateButton.setDisable(false);
           logoutButton.setDisable(false);  // added, KF
           //save the bracket along with account info
           seralizeBracket(selectedBracket);

       }else{
            infoAlert("You can only finalize a bracket once it has been completed.");
            //go back to bracket section selection screen
            displayPane(bracketPane);

       }

    }


    /**
     * displays element in the center of the screen
     *
     * @param p must use a subclass of Pane for layout.
     * to be properly center aligned in  the parent node
     */
    private void displayPane(Node p){
        root.setCenter(p);
        BorderPane.setAlignment(p,Pos.CENTER);
    }

    /**
     * Creates toolBar and buttons.
     * adds buttons to the toolbar and saves global references to them
     */
    private void CreateToolBars(){
        toolBar  = new ToolBar();
        btoolBar  = new ToolBar();
        saveButton = new Button("Save"); // added KF
        logoutButton =new Button("Logout"); // added KF
        simulateButton =new Button("Simulate"); // added KF
        scoreBoardButton=new Button("ScoreBoard");
        viewPredictedBracketButton = new Button("View Predicted Bracket"); // added KF
        viewSimulatedBracketButton = new Button("View Simulated Bracket");
        clearButton=new Button("Clear");
        finalizeButton=new Button("Finalize");
        helpButton=new Button("Help"); // added AE
        toolBar.getItems().addAll(
                createSpacer(),
                helpButton, // added AE
                saveButton, // added KF
                logoutButton, // added KF
                simulateButton,
                scoreBoardButton,
                viewPredictedBracketButton, // added KF
                viewSimulatedBracketButton,
                createSpacer()
        );
        btoolBar.getItems().addAll(
                createSpacer(),
                clearButton,
      //          resetButton,
                finalizeButton,
                back=new Button("Choose Division"),
                createSpacer()
        );
    }

   /**
    * sets the actions for each button
    */
    private void setActions(){
        saveButton.setOnAction(e->save()); // added KF
        logoutButton.setOnAction(e->logout()); // added KF
        simulateButton.setOnAction(e->simulate());
        scoreBoardButton.setOnAction(e->scoreBoard());
        viewPredictedBracketButton.setOnAction(e-> viewPredictedBracket()); // added KF
        viewSimulatedBracketButton.setOnAction(e->viewSimulatedBracket());
        clearButton.setOnAction(e->clear());
        // removed resetButton Dov
        finalizeButton.setOnAction(e->finalizeBracket());
        helpButton.setOnAction(e->help());
        back.setOnAction(e->{
            bracketPane=new BracketPane(selectedBracket);
            displayPane(bracketPane);
        });
    }

    /**
     * Saves an unfinished bracket to the .ser file of the user
     */
    private void save() {
        seralizeBracket(selectedBracket);
    }

    /**
     * Creates a spacer for centering buttons in a ToolBar
     */
    private Pane createSpacer(){
        Pane spacer = new Pane();
        HBox.setHgrow(
                spacer,
                Priority.SOMETIMES
        );
        return spacer;
    }
    
    
    private GridPane createLogin(){
        
        
        /*
        LoginPane
        Sergio and Joao
         */

        GridPane loginPane = new GridPane();
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(5, 5, 5, 5));

        Text welcomeMessage = new Text("March Madness Login Welcome");
        loginPane.add(welcomeMessage, 0, 0, 2, 1);

        Label userName = new Label("User Name: ");
        loginPane.add(userName, 0, 1);

        // removed TextField enterUser KF
        String[] userList = getUsers();  // added to get list of saved users in directory -KF
        ComboBox<String> enterUser = new ComboBox<String>(FXCollections.observableArrayList(userList)); //adds a ComboBox to the pane for name entry -KF
        loginPane.add(enterUser, 1, 1);
        enterUser.setEditable(true); // allows you to still enter new names - KF

        Label password = new Label("Password: ");
        loginPane.add(password, 0, 2);

        PasswordField passwordField = new PasswordField();
        loginPane.add(passwordField, 1, 2);

        Button signButton = new Button("Sign in");
        loginPane.add(signButton, 1, 4);
        signButton.setDefaultButton(true);//added by matt 5/7, lets you use sign in button by pressing enter

        Label message = new Label();
        loginPane.add(message, 1, 5);

        signButton.setOnAction(event -> {

            // the name user enter
            String name = enterUser.getValue();
            // the password user enter
            String playerPass = passwordField.getText();

            if (Arrays.asList(userList).contains(name)) {
                //check password of user
                 
                Bracket fileBracket = deseralizeBracket(name + ".ser"); // loads Bracket from file KF
                String fileBracketPassword = fileBracket.getPassword();

                try {
                    if (fileBracketPassword.equals(hashText(playerPass))) {  // added hashing for passwords
                        // load bracket
                        selectedBracket=fileBracket;
                        enterUser.setValue("");  // clears name from combobox KF
                        passwordField.setText("");  // clears password from textfield KF
                        saveButton.setDisable(false);  // added KF
                        logoutButton.setDisable(false); // added KF
                        chooseBracket();
                    }else{
                       infoAlert("The password you have entered is incorrect!");
                    }
                } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

            } else {
                //check for empty fields
                if(!name.equals("")&&!playerPass.equals("")){
                    //create new bracket
                    Bracket tmpPlayerBracket = new Bracket(startingBracket, name);
                    // removed playerBrackets.add(tmpPlayerBracket) as we're not loading all brackets at once KF
                    try {
                        tmpPlayerBracket.setPassword(hashText(playerPass));  //more hashing for passwords
                    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }

                    // removed playerMap.put(name, tmpPlayerBracket) as we're not loading all brackets at once KF
                    selectedBracket = tmpPlayerBracket;
                    enterUser.getItems().add(name);
                    //alert user that an account has been created
                    infoAlert("No user with the Username \""  + name + "\" exists. A new account has been created.");
                    chooseBracket();
                }
            }
        });
        
        return loginPane;
    }

    /**
     * Function to return a String array of all usernames via looking at the directory and finding all .ser files
     * @return
     * Katherine Foley
     */
    private String[] getUsers() {
        File userDir = new File(".");
        String[] files = userDir.list();
        LinkedList<String> userList = new LinkedList<String>();
        for (String s: files) {
            if (s.endsWith(".ser")) {
                userList.add(s.split(".ser")[0]);
            }
        }
        return userList.toArray(new String[0]);

    }

    // removed addAllToMap as we are not loading all brackets into memory anymore KF
    
    /**
     * The Exception handler
     * Displays a error message to the user
     * and if the error is bad enough closes the program
     * @param fatal true if the program should exit. false otherwise
     */
    private void showError(Exception e,boolean fatal){
        String msg=e.getMessage();
        if(fatal){
            msg=msg+" \n\nthe program will now close";
            //e.printStackTrace();
        }
        Alert alert = new Alert(AlertType.ERROR,msg);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(420);   
        alert.setTitle("Error");
        alert.setHeaderText("something went wrong");
        alert.showAndWait();
        if(fatal){ 
            System.exit(666);
        }   
    }
    
    /**
     * alerts user to the result of their actions in the login pane 
     * @param msg the message to be displayed to the user
     */
    private void infoAlert(String msg){
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("March Madness Bracket Simulator");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    // removed confirmReset as no longer used KF

    /**
     * Tayon Watson 5/5
     * seralizedBracket
     * @param B The bracket the is going to be seralized
     */
    private void seralizeBracket(Bracket B){
        FileOutputStream outStream = null;
        ObjectOutputStream out = null;
    try 
    {
      outStream = new FileOutputStream(B.getPlayerName()+".ser");
      out = new ObjectOutputStream(outStream);
      out.writeObject(B);
      out.close();
    } 
    catch(IOException e)
    {
      // Grant osborn 5/6 hopefully this never happens 
      showError(new Exception("Error saving bracket \n"+e.getMessage(),e),false);
    }
    }
    /**
     * Tayon Watson 5/5
     * deseralizedBracket
     * @param filename of the seralized bracket file
     * @return deserialized bracket 
     */
    private Bracket deseralizeBracket(String filename){
        Bracket bracket = null;
        FileInputStream inStream = null;
        ObjectInputStream in = null;
    try 
    {
        inStream = new FileInputStream(filename);
        in = new ObjectInputStream(inStream);
        bracket = (Bracket) in.readObject();
        in.close();
    }catch (IOException | ClassNotFoundException e) {
      // Grant osborn 5/6 hopefully this never happens either
      showError(new Exception("Error loading bracket \n"+e.getMessage(),e),false);
    } 
    return bracket;
    }
    
      /**
     * Tayon Watson 5/5
     * deseralizedBracket
     * @return deserialized bracket
     */
    private ArrayList<Bracket> loadAllBrackets()
    {   
        ArrayList<Bracket> list=new ArrayList<Bracket>();
        File dir = new File(".");
        for (final File fileEntry : dir.listFiles()){
            String fileName = fileEntry.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".")+1);
       
            if (extension.equals("ser")){
                list.add(deseralizeBracket(fileName));
            }
        }
        return list;
    }

    /**
     * Katherine Foley 4/4/2023
     * function for hashing plaintext password for storage
     * @param text
     * @return
     * @throws Exception
     */
    private String hashText(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] hashBytes;
        String hashString;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        hashBytes = md.digest(text.getBytes());
        hashString = new String(hashBytes, "UTF-8");
        return hashString;
    }


}
