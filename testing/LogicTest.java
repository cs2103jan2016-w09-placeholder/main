import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.Test;

import common.*;
import junitx.framework.FileAssert;
import logic.Logic;

public class LogicTest {
    
    Logic logic = Logic.getInstance();

    @Test
    public void addTask_MissingDescription_InvalidCommand() {
        Result noSpace = logic.processCommand("add");
        assertEquals(Command.CommandType.ADD, noSpace.getCommandType());
        assertEquals(false, noSpace.isSuccess());
        
        Result noDescAfterSpace = logic.processCommand("add  ");
        assertEquals(Command.CommandType.ADD, noDescAfterSpace.getCommandType());
        assertEquals(false, noSpace.isSuccess());
        
        Result noDescWithDate = logic.processCommand("add end today");
        assertEquals(Command.CommandType.ADD, noDescWithDate.getCommandType());
        assertEquals(false, noSpace.isSuccess());
    }
    
    @Test
    public void addTask_InvalidDate_InvalidCommand() {
        Result laterStart = logic.processCommand("add 111 start 01/01/2016 12:00 end 31/12/2015 12:00");
        assertEquals(Command.CommandType.INVALID, laterStart.getCommandType());
    }
    
    @Test
    public void addTask_AllTaskTypes_AddedInOrder() {
        createEmptyFile("output_addTask_AllTaskTypes.txt");
        logic.processCommand("load output_addTask_AllTaskTypes.txt");
        
        logic.processCommand("add Hello");
        logic.processCommand("add Goodbye");
        logic.processCommand("add Meeting start 24/05/2016 14:00");
        logic.processCommand("add Meeting end 25/05/2016 14:00");
        logic.processCommand("add Meeting end 24/05/2016 14:00");
        logic.processCommand("add Meeting start 24/05/2016 12:00 end 26/05/2016 14:00");
        
        File expectedFile = new File("expected_addTask_AllTaskTypes.txt");
        File actualFile = new File("output_addTask_AllTaskTypes.txt");
        FileAssert.assertEquals(expectedFile, actualFile);
    }
    
    @Test
    public void deleteTask_ValidTaskNumber_DeletedSelectedTask() {
        logic.processCommand("load input_deleteTask_ValidTaskNumber.txt");
        logic.processCommand("save output_deleteTask_ValidTaskNumber.txt");
        
        logic.processCommand("delete 6");
        logic.processCommand("delete 1");
        logic.processCommand("delete 1");
        
        File expectedFile = new File("expected_deleteTask_ValidTaskNumber.txt");
        File actualFile = new File("output_deleteTask_ValidTaskNumber.txt");
        FileAssert.assertEquals(expectedFile, actualFile);
    }
        
    @Test
    public void editTask_ValidTaskNumber_EditedSelectedTask() {
        logic.processCommand("load input_editTask_AllTaskTypes.txt");
        logic.processCommand("save output_editTask_AllTaskTypes.txt");
        
        logic.processCommand("edit 6 Meeting start 24/05/2016 12:00 end 30/05/2016 14:00");
        logic.processCommand("edit 6 Hello world");
        
        File expectedFile = new File("expected_editTask_AllTaskTypes.txt");
        File actualFile = new File("output_editTask_AllTaskTypes.txt");
        FileAssert.assertEquals(expectedFile, actualFile);
    }

    @Test
    public void saveTasks_WithFileName_Success() {
        createEmptyFile("output_saveTasks_WithFileName.txt");
        logic.processCommand("load output_saveTasks_WithFileName.txt");
        logic.processCommand("add Meeting start 24/05/2016 12:00 end 26/05/2016 14:00");
        logic.processCommand("add Meeting end 24/05/2016 14:00");
        logic.processCommand("add Goodbye");
        logic.processCommand("add Meeting start 24/05/2016 14:00");
        logic.processCommand("done 1");
        logic.processCommand("done 1");
        logic.processCommand("done 1");
        logic.processCommand("done 1");
        logic.processCommand("add Meeting start 24/05/2016 12:00 end 26/05/2016 14:00");
        logic.processCommand("add Meeting end 24/05/2016 14:00");
        logic.processCommand("add Goodbye");
        logic.processCommand("add Meeting start 24/05/2016 14:00");
        
        Result result = logic.processCommand("save output_saveTasks_WithFileName.txt");
        assertEquals(Command.CommandType.SAVE, result.getCommandType());
        assertEquals(true, result.isSuccess());
        
        File expectedFile = new File("expected_saveTasks_WithFileName.txt");
        File actualFile = new File("output_saveTasks_WithFileName.txt");
        FileAssert.assertEquals(expectedFile, actualFile);
    }

    @Test
    public void loadTasks_WithFileName_Success() {
        Result result = logic.processCommand("load input_loadTasks_WithFileName.txt");
        assertEquals(Command.CommandType.LOAD, result.getCommandType());
        assertEquals(true, result.isSuccess());
        
        ArrayList<Task> expectedMainList = new ArrayList<Task>();
        expectedMainList.add(new Task("Meeting", LocalDateTime.of(2016, 05, 24, 12, 0), 
                LocalDateTime.of(2016, 05, 26, 14, 0), 1));
        expectedMainList.add(new Task("Meeting", null, 
                LocalDateTime.of(2016, 05, 24, 14, 0), 2));
        expectedMainList.add(new Task("Goodbye", null, null, 3));
        expectedMainList.add(new Task("Meeting", LocalDateTime.of(2016, 05, 24, 14, 0), null, 4));
        
        ArrayList<Task> actualMainList = logic.getMainList();
        assertArrayEquals(expectedMainList.toArray(), actualMainList.toArray());

        ArrayList<Task> expectedDoneList = new ArrayList<Task>();
        expectedDoneList.add(new Task("Meeting", LocalDateTime.of(2016, 05, 24, 12, 0), 
                LocalDateTime.of(2016, 05, 26, 14, 0), 1));
        expectedDoneList.add(new Task("Meeting", null, 
                LocalDateTime.of(2016, 05, 24, 14, 0), 2));
        expectedDoneList.add(new Task("Goodbye", null, null, 3));
        expectedDoneList.add(new Task("Meeting", LocalDateTime.of(2016, 05, 24, 14, 0), null, 4));
        
        ArrayList<Task> actualDoneList = logic.getDoneList();
        assertArrayEquals(expectedDoneList.toArray(), actualDoneList.toArray());
    }
    
    @Test
    public void search_NoParam_DisplayAll() {
        logic.processCommand("load input_search.txt");
        File expectedFile = new File("expected_search_NoParam.txt");
        File actualFile;
        
        Result noSpace = logic.processCommand("search");
        actualFile = saveSearchResults("output_search_NoParam", noSpace.getResults());
        assertEquals(Command.CommandType.SEARCH, noSpace.getCommandType());
        FileAssert.assertEquals(expectedFile, actualFile);

        Result hasWhitespace = logic.processCommand("search  ");
        actualFile = saveSearchResults("output_search_NoParam.txt", hasWhitespace.getResults());
        assertEquals(Command.CommandType.SEARCH, hasWhitespace.getCommandType());
        FileAssert.assertEquals(expectedFile, actualFile);
    }
    
    @Test
    public void search_KeywordExists_DisplayMatching() {
        logic.processCommand("load input_search.txt");
        File expectedFile = new File("expected_search_KeywordExists.txt");
        File actualFile;
        
        Result word = logic.processCommand("search aAA");
        actualFile = saveSearchResults("output_search_KeywordExists.txt",
                word.getResults());
        FileAssert.assertEquals(expectedFile, actualFile);

        Result wordWithWhitespace = logic.processCommand("search aAA   ");
        actualFile = saveSearchResults("output_search_KeywordExists.txt",
                wordWithWhitespace.getResults());
        FileAssert.assertEquals(expectedFile, actualFile);
    }
    
    @Test
    public void search_DateExists_DisplayMatching() {
        logic.processCommand("load input_search.txt");
        File expectedFile = new File("expected_search_DateExists.txt");
        File actualFile;
        
        Result date = logic.processCommand("search 07/07/2016");
        actualFile = saveSearchResults("output_search_DateExists.txt",
                date.getResults());
        FileAssert.assertEquals(expectedFile, actualFile);

        Result dateWithWhitespace = logic.processCommand("search 07/07/2016   ");
        actualFile = saveSearchResults("output_search_DateExists.txt",
                dateWithWhitespace.getResults());
        FileAssert.assertEquals(expectedFile, actualFile);
    }
    
    @Test
    public void doneTask_ValidTaskNumber_MarkAsDone() {
        logic.processCommand("load input_doneTask_ValidTaskNumber.txt");
        logic.processCommand("save output_doneTask_ValidTaskNumber.txt");
        
        logic.processCommand("done 6");
        logic.processCommand("done 1");
        logic.processCommand("done 1");
        
        File expectedFile = new File("expected_doneTask_ValidTaskNumber.txt");
        File actualFile = new File("output_doneTask_ValidTaskNumber.txt");
        FileAssert.assertEquals(expectedFile, actualFile);
    }
    
    private static File createEmptyFile(String fileName) {
        File file = new File(fileName);
        try (BufferedWriter buffWriter = new BufferedWriter(new FileWriter(file))) {
            buffWriter.write("");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return file;
    }
    
    private static File saveSearchResults(String fileName, ArrayList<Task> results) {
        File file = createEmptyFile(fileName);
        try (BufferedWriter buffWriter = new BufferedWriter(new FileWriter(file))) {
            for (Task task : results) {
                String str = task.toString();
                buffWriter.append(str);
                buffWriter.newLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return file;
    }
    
}
