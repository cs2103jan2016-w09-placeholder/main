//@@author Ruoling
package ui;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.controlsfx.control.HiddenSidesPane;

import common.Command.CommandType;
import common.Result;
import common.Task;
import ui.MainApp;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class DisplayController extends HiddenSidesPane {

    private static Logger logger = Logger.getLogger("MainApp.DisplayController");
    private MainApp main;
    private Stage primaryStage;
    
    private VBox taskPanel, searchPanel, completedPanel, helpPanel;
    private SidebarController sidebar;
    private Popup feedback;
    
    private static final String HEADER_COMPLETED_SINGLE = " completed task";
    private static final String HEADER_COMPLETED_PLURAL = " completed tasks";
    private static final String HEADER_SEARCH_SINGLE = " search result found";
    private static final String HEADER_SEARCH_PLURAL = " search results found";
    
    private static final String FXML_DISPLAY = "Display.fxml";
    private static final String RESOURCES_ICON_SUCCESS = "/icons/success-small.png";
    private static final String RESOURCES_ICON_SUCCESS_DELETE = "/icons/delete-success-small.png";
    private static final String RESOURCES_ICON_SUCCESS_SAVE = "/icons/save-success-small.png";
    private static final String RESOURCES_ICON_FAIL = "/icons/fail-small.png";
    private static final String RESOURCES_ICON_FAIL_DELETE = "/icons/delete-fail-small.png";
    private static final String RESOURCES_ICON_FAIL_SAVE = "/icons/save-fail-small.png";

    private static final int TYPE_COMPLETED = 1;
    private static final int TYPE_OTHER = 2;
    private static final int TYPE_TODAY = 3;

    public DisplayController(MainApp main, Stage primaryStage) {
        this.main = main;
        this.primaryStage = primaryStage;
        this.setFocusTraversable(false);
        //initializeLogger();
        loadFXML();
        initializeTaskPanel();
        initializeSearchPanel();
        initializeCompletedPanel();
        initializeHelpPanel();
        initializeSidebar();
        initializePopup();
        handleUserInteractions();
    }

    private void initializeLogger() {
        try {
            Handler fh = new FileHandler("log_ui_display");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_DISPLAY));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to load Display", e);
        }
    }
    
    private void initializeTaskPanel() {
        taskPanel = new VBox();
        taskPanel.getStyleClass().add("panel");
        ArrayList<Task> allTasks = main.getTasks();
        updateTaskPanel(allTasks);
        this.setContent(taskPanel);
    }

    private void initializeSearchPanel() {
        searchPanel = new VBox();
        searchPanel.getStyleClass().add("panel");
    }
    
    private void initializeCompletedPanel() {
        completedPanel = new VBox();
        completedPanel.getStyleClass().add("panel");
    }
    
    private void initializeHelpPanel() {
        helpPanel = new VBox();
        helpPanel.getStyleClass().add("panel");
        Label helpHeader = createHeader("Commands");
        ScrollPane helpContent = createHelpContent();
        helpPanel.getChildren().addAll(helpHeader, helpContent);
    }

    private ScrollPane createHelpContent() {
        String[] commands = { "Add a task",
                              "Edit a task",
                              "Search tasks",
                              "Mark as done",
                              "Access done tasks",
                              "Delete a task",
                              "Undo",
                              "Redo",
                              "Show help guide",
                              "Exit",
                              "Save file",
                              "Load file",
                              "View sidebar" };
        
        String[] formats = { "add <description>",
                             "edit <task number> <new description> <new timing> #<new category>",
                             "search <keyword / date / #category>",
                             "done <task number>",
                             "searchdone <keyword / date / #category>",
                             "delete <task number>",
                             "undo",
                             "redo",
                             "help",
                             "exit",
                             "save <directory> <filename>",
                             "load <directory> <filename>",
                             "CTRL+M" };

        assert (commands.length == formats.length);
        
        GridPane table = new GridPane();
        table.getStyleClass().add("table");
        
        // set column widths to 30% for first col and 70% for second col
        ColumnConstraints commandCol = new ColumnConstraints();
        commandCol.setPercentWidth(30);
        ColumnConstraints formatCol = new ColumnConstraints();
        formatCol.setPercentWidth(70);
        table.getColumnConstraints().addAll(commandCol, formatCol);
        
        // add command and format row by row
        for (int i = 0; i < commands.length; i++) {
            Label cmd = new Label(commands[i]);
            table.add(cmd, 0, i);
            Label format = new Label(formats[i]);
            table.add(format, 1, i);
        }
        
        // make table scrollable
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(table);
        return scrollPane;
    }

    private void initializeSidebar() {
        sidebar = new SidebarController(main);
        this.setLeft(sidebar);
        this.setTriggerDistance(30);
    }
    
    private void initializePopup() {
        feedback = new Popup();
        feedback.setAutoHide(true);
        // hide feedback popup on any key press
        feedback.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                feedback.hide();
            }
        });
    }
    
    private void handleUserInteractions() {
        DisplayController display = this;
        // allow keyboard shortcuts
        display.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                if (event.getCode()==KeyCode.M && event.isControlDown()) {
                    logger.log(Level.INFO, "user toggled sidebar");
                    if (display.getPinnedSide() == Side.LEFT) {
                        display.setPinnedSide(null);
                    } else {
                        display.setPinnedSide(Side.LEFT);
                    }
                }
            }
        });
    }
    
    /**
     * Displays the result of the command to the user.
     * 
     * @param result    the result to be shown to the user
     */
    public void displayResult(Result result) {
        assert (result != null);
        CommandType cmdType = result.getCommandType();
        switch(cmdType) {
            case INVALID :
                showFeedback(cmdType, result.getMessage(), result.isSuccess());
                break;

            case SEARCH :
                updateSearchPanel(result.getResults());
                this.setContent(searchPanel);
                break;
                
            case SEARCHDONE :
                updateCompletedPanel(result.getResults());
                this.setContent(completedPanel);
                break;
            
            case HELP :
                this.setContent(helpPanel);
                break;
            
            default :
                sidebar.update();
                updateTaskPanel(result.getResults());
                showFeedback(cmdType, result.getMessage(), result.isSuccess());
                this.setContent(taskPanel);
                break;
        }
    }

    /******************
     * HELPER METHODS *
     ******************/
    
    private void updateTaskPanel(ArrayList<Task> allTasks) {
        assert (allTasks != null);
        
        LocalDateTime todayDateTime = LocalDateTime.now();
        LocalDate todayDate = todayDateTime.toLocalDate();
        // group tasks in Today or Others section
        ObservableList<VBox> todayTasks = FXCollections.observableArrayList();
        ObservableList<VBox> otherTasks = FXCollections.observableArrayList();
        // index of tasks to scroll to
        int todayModified = -1;
        int otherModified = -1;
        for (Task task : allTasks) {
            if (task.isOccurringOn(todayDate)) {
                todayTasks.add(createTodayEntry(task, todayDateTime));
                if (task.isModified()) {
                    todayModified = todayTasks.size() - 1;
                }
            } else {
                otherTasks.add(createOtherEntry(task, todayDateTime));
                if (task.isModified()) {
                    otherModified = otherTasks.size() - 1;
                }
            }
        }
        
        // build today panel
        Label todayHeader = createHeader("Today");
        VBox todayContent = new VBox();
        if (todayTasks.isEmpty()) {
            Label empty = new Label("No tasks today");
            todayContent.getChildren().add(empty);
        } else {
            ListView<VBox> todayListView = createListView(todayTasks);
            todayContent.getChildren().add(todayListView);
            todayListView.setMaxHeight(primaryStage.getHeight()/2.1);
            if (todayModified != -1) {
                todayListView.scrollTo(todayModified);
            }
        }
        
        VBox todayPanel = new VBox();
        todayPanel.getChildren().addAll(todayHeader, todayContent);
        
        // build other panel
        Label otherHeader = createHeader("Others");
        VBox otherContent = new VBox();
        if (otherTasks.isEmpty()) {
            Label empty = new Label("No tasks today");
            otherContent.getChildren().add(empty);
        } else {
            ListView<VBox> otherListView = createListView(otherTasks);
            otherContent.getChildren().add(otherListView);
            otherListView.setMaxHeight(primaryStage.getHeight()/2.1);
            if (otherModified != -1) {
                otherListView.scrollTo(otherModified);
            }
        }
        
        VBox otherPanel = new VBox();
        otherPanel.getChildren().addAll(otherHeader, otherContent);
        
        // add to task panel
        taskPanel.getChildren().clear();
        taskPanel.getChildren().addAll(todayPanel, otherPanel);
    }

    private void updateSearchPanel(ArrayList<Task> results) {
        assert (searchPanel != null);
        LocalDateTime todayDate = LocalDateTime.now();
        
        Label searchHeader = createHeader(results.size() + (results.size() == 1 ? HEADER_SEARCH_SINGLE : HEADER_SEARCH_PLURAL));
        ObservableList<VBox> searchTasks = FXCollections.observableArrayList();
        for (Task result : results) {
            searchTasks.add(createOtherEntry(result, todayDate));
        }
        ListView<VBox> searchListView = createListView(searchTasks);
        
        searchPanel.getChildren().clear();
        searchPanel.getChildren().addAll(searchHeader, searchListView);
    }
    
    private void updateCompletedPanel(ArrayList<Task> results) {
        assert (completedPanel != null);

        Label completedHeader = createHeader(results.size() + (results.size() == 1 ? HEADER_COMPLETED_SINGLE : HEADER_COMPLETED_PLURAL));
        ObservableList<VBox> completedTasks = FXCollections.observableArrayList();
        for (Task result : results) {
            completedTasks.add(createCompleted(result));
        }
        ListView<VBox> completedListView = createListView(completedTasks);
        
        completedPanel.getChildren().clear();
        completedPanel.getChildren().addAll(completedHeader, completedListView);
    }

    private void showFeedback(CommandType cmd, String msg, boolean isSuccess) {
        logger.log(Level.INFO, String.format("showing feedback for %1s, %2s", cmd, isSuccess));

        HBox box = createFeedback(cmd, msg, isSuccess);
        feedback.getContent().clear();
        feedback.getContent().add(box);
        double x = primaryStage.getX() + 10;
        double y = primaryStage.getY() + primaryStage.getHeight();
        feedback.setX(x);
        feedback.setY(y);
        feedback.show(primaryStage);
    }
    
    private HBox createFeedback(CommandType commandType, String msg, boolean isSuccess) {
        HBox box = new HBox();
        box.getStylesheets().add(getClass().getResource("feedback.css").toExternalForm()); 
        Text message = new Text(msg);
        ImageView icon;
        
        if (isSuccess) {
            box.setId("popup-success");
            message.setId("popup-success-text");
            switch(commandType) {
                case DELETE :
                    icon = new ImageView(new Image(RESOURCES_ICON_SUCCESS_DELETE));
                    break;
                    
                case SAVE :
                    icon = new ImageView(new Image(RESOURCES_ICON_SUCCESS_SAVE));
                    break;
                    
                default :
                    icon = new ImageView(new Image(RESOURCES_ICON_SUCCESS));
                    break;
            }
        } else {
            box.setId("popup-fail");
            message.setId("popup-fail-text");
            switch(commandType) {
                case DELETE :
                icon = new ImageView(new Image(RESOURCES_ICON_FAIL_DELETE));
                break;
                
                case SAVE :
                icon = new ImageView(new Image(RESOURCES_ICON_FAIL_SAVE));
                break;
                
                default :
                icon = new ImageView(new Image(RESOURCES_ICON_FAIL));
                break;
            }
        }
        box.getChildren().addAll(icon, message);
        return box;
        
    }
    
    private Label createHeader(String heading) {
        Label header = new Label(heading);
        header.getStyleClass().add("header");
        header.setMinHeight(USE_PREF_SIZE);
        return header;
    }

    private ListView<VBox> createListView(ObservableList<VBox> tasks) {
        ListView<VBox> listView = new ListView<VBox>(tasks);
        listView.prefHeightProperty().bind(Bindings.size(tasks).multiply(65));
        return listView;
    }
    
    private VBox createTodayEntry(Task task, LocalDateTime todayDate) {
        Label desc = new Label(task.getId() + ". " + task.getDescription());
        HBox details = createTaskDetails(task, TYPE_TODAY, todayDate);
        
        VBox entry = new VBox();
        entry.getChildren().addAll(desc, details);
        if (task.isModified()) {
            entry.getStyleClass().add("entry-task-modified");
        } else if (task.isOverdue(todayDate)) {
            entry.getStyleClass().add("entry-task-overdue");
        } else {
            entry.getStyleClass().add("entry-task");
        }
        return entry;
    }
    
    private VBox createOtherEntry(Task task, LocalDateTime todayDate) {
        Label desc = new Label(task.getId() + ". " + task.getDescription());
        HBox details = createTaskDetails(task, TYPE_OTHER, todayDate);

        VBox entry = new VBox();
        entry.getChildren().addAll(desc, details);
        if (task.isModified()) {
            entry.getStyleClass().add("entry-task-modified");
        } else if (task.isOverdue(todayDate)) {
            entry.getStyleClass().add("entry-task-overdue");
        } else {
            entry.getStyleClass().add("entry-task");
        }
        return entry;
    }
    
    private VBox createCompleted(Task task) {
        Label desc = new Label(task.getDescription());
        HBox details = createTaskDetails(task, TYPE_COMPLETED, null);
        
        VBox entry = new VBox();
        entry.getChildren().addAll(desc, details);
        entry.getStyleClass().add("entry-task");
        return entry;
    }
    
    private HBox createTaskDetails(Task task, int type, LocalDateTime date) {
        HBox details = new HBox();
        
        // add priority icon
        
        // add date details
        Label startEndDate = null;
        
        if (type == TYPE_COMPLETED) {
            startEndDate = createCompletedTaskDetails(task);
        } else {
            startEndDate = createOngoingTaskDetails(task, date);
        }
        
        if (startEndDate != null) {
            startEndDate.getStyleClass().add("details");
            details.getChildren().add(startEndDate);
        }
        
        // add reminder icon and date
        
        // add categories
        
        return details;
    }

    private Label createOngoingTaskDetails(Task task, LocalDateTime date) {
        Label startEndDate = null;
        LocalDate now = date.toLocalDate();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        if (task.isDeadline()) {
            LocalDateTime end = task.getEndDate();
            String endDate;
            if (end.toLocalDate().isEqual(now)) {
                endDate = timeFormatter.format(end);
            } else {
                endDate = dateFormatter.format(end); 
            }
            startEndDate = new Label("By " + endDate);
        } else if (task.isEvent()) {
            LocalDateTime start = task.getStartDate();
            LocalDateTime end = task.getEndDate();
            String startDate, endDate;
            if (start.toLocalDate().isEqual(now)) {
                startDate = timeFormatter.format(start);
            } else {
                startDate = dateFormatter.format(start);
            }
            if (end.toLocalDate().isEqual(now)) {
                endDate = timeFormatter.format(end);
            } else {
                endDate = dateFormatter.format(end);
            }
            startEndDate = new Label("From " + startDate + " to " + endDate);
        }
        return startEndDate;
    }

    private Label createCompletedTaskDetails(Task task) {
        Label startEndDate = null;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (task.isDeadline()) {
            String endDate = dateFormatter.format(task.getEndDate());
            startEndDate = new Label("By " + endDate);
        } else if (task.isEvent()) {
            LocalDateTime start = task.getStartDate();
            LocalDateTime end = task.getEndDate();
            String startDate = dateFormatter.format(start);
            String endDate = dateFormatter.format(end);
            startEndDate = new Label("From " + startDate + " to " + endDate);
        }
        return startEndDate;
    }
    
}
