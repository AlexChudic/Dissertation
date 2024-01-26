package refactoring_mining;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.json.JSONArray;
import org.json.JSONObject;

public class HelperTools {
    
    public static void replaceFile(String filePath, String newContent){
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(newContent);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String preprocessLLMRefactoring(String LLMRefactoring){
        return LLMRefactoring.substring(8,LLMRefactoring.length()-3);
    }

    public static void checkout(Repository repository, String branch, String commitId) throws Exception {
        System.out.println("Checking out " + repository.getDirectory().getParent().toString() + " " + commitId + " ...");
        try (Git git = new Git(repository)) {
            git.reset()
                .setRef("refs/remotes/origin/" + branch)
                .setMode(ResetType.HARD)
                .call();
            CheckoutCommand checkout = git.checkout().setName(commitId).setForced(true);
            checkout.call();
        }
    }

    public static void getLLMRefactoring(String JSONPath){
        String[] command = {"runModel/bin/python", "src/useGPT.py", JSONPath};
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
                    
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
                throw new RuntimeException("Python script execution failed with exit code: " + exitCode);
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("Python script executed successfully");
            }
                    
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getFileFromJSON(String JSONPath, String file){
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(JSONPath)));
            JSONObject json = new JSONObject(jsonString);
            // Get the value of LLMRefactoring.simplePrompt
            if (file == "LLM_simple"){
                if (json.has("LLMRefactoring") && json.getJSONObject("LLMRefactoring").has("simplePrompt")) {
                    return json.getJSONObject("LLMRefactoring").getString("simplePrompt");
                } else {
                    System.out.println(JSONPath+ " LLMRefactoring.simplePrompt not found in the JSON.");
                }
            } else if(file == "before"){
                if (json.has("beforeRefactoring") && json.getJSONObject("beforeRefactoring").has("file")) {
                    return convertArrayOfStringsToSingleString(json.getJSONObject("beforeRefactoring").getJSONArray("file"));
                } else {
                    System.out.println(JSONPath+ " beforeRefactoring.file not found in the JSON.");
                }
            } else if(file == "after"){
                if (json.has("afterRefactoring") && json.getJSONObject("afterRefactoring").has("file")) {
                    return convertArrayOfStringsToSingleString(json.getJSONObject("afterRefactoring").getJSONArray("file"));
                } else {
                    System.out.println(JSONPath+ " afterRefactoring.file not found in the JSON.");
                }
            } else {
                System.out.println("Invalid file type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getChangedFilePath(String JSONPath){
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(JSONPath)));
            JSONObject json = new JSONObject(jsonString);
            // Get the value of before
            if (json.has("beforeRefactoring") && json.getJSONObject("beforeRefactoring").has("filePath")) {
                return json.getJSONObject("beforeRefactoring").getString("filePath");
            } else {
                System.out.println(JSONPath + " changed file path not found in the JSON.");
            }
        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + JSONPath);
            e.printStackTrace();
        }
        return null;
    }
    public static boolean isSingleFileRefactoring(String JSONPath){
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(JSONPath)));
            JSONObject json = new JSONObject(jsonString);
            // Get the value of before
            if (json.has("beforeRefactoring") && json.getJSONObject("beforeRefactoring").has("filePath") &&
                json.has("afterRefactoring") && json.getJSONObject("afterRefactoring").has("filePath") &&
                json.getJSONObject("beforeRefactoring").getString("filePath").equals(json.getJSONObject("afterRefactoring").getString("filePath"))) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + JSONPath);
            e.printStackTrace();
        }
        return false;
    }

    public static String convertArrayOfStringsToSingleString(JSONArray array){
        String result = "";
        for (int i = 0; i < array.length(); i++) {
            String string = array.getString(i);
            result += string + "\n";
        }
        return result;
    }

}