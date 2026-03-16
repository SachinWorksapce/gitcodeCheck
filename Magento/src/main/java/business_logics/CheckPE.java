
package business_logics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.nlp.util.Nlp;
import com.tyss.optimize.nlp.util.NlpException;
import com.tyss.optimize.nlp.util.NlpRequestModel;
import com.tyss.optimize.nlp.util.NlpResponseModel;
import com.tyss.optimize.nlp.util.annotation.InputParam;
import com.tyss.optimize.nlp.util.annotation.InputParams;

public class CheckPE implements Nlp {
	  private static final String PYTHON_EXE = "python";  // or full path
	    private static final String ADB_PATH = "adb";       // or full path
	    private static final String PY_FILE_NAME = "dictateText.py";

    @InputParams({@InputParam(name = "Text to Dictate", type = "java.lang.String")})
    

      public NlpResponseModel execute(NlpRequestModel nlpRequestModel) throws NlpException {

          Map<String, Object> programElementsInput = nlpRequestModel.getAttributes();
          String texttoDictate = (String) programElementsInput.get("Text to Dictate");
          NlpResponseModel nlpResponseModel = new NlpResponseModel();
          try {
        	  generateAndPlay(texttoDictate);
              nlpResponseModel.setMessage(texttoDictate+" played successfully");

              nlpResponseModel.setStatus(CommonConstants.pass);
          }
          catch(Exception e) {
       		StringWriter sw = new StringWriter();
  			e.printStackTrace(new PrintWriter(sw));
  			String exceptionAsString = sw.toString();
  			nlpResponseModel.setStatus(CommonConstants.fail);
  			nlpResponseModel.setMessage("Failed to convert and play the text " + exceptionAsString);
           nlpResponseModel.setStatus(CommonConstants.fail);
          }

          return nlpResponseModel;
      }
    public static void generateAndPlay(String textToDictate) throws Exception {

        // 1️⃣ Get system temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        File pythonFile = new File(tempDir, PY_FILE_NAME);

        // 2️⃣ Create only if not exists
        if (!pythonFile.exists()) {
            writePythonScript(pythonFile);
            System.out.println("Python script created at: " + pythonFile.getAbsolutePath());
        } else {
            System.out.println("Reusing existing Python script.");
        }

        // 3️⃣ Execute: python dictateText.py "text"
        ProcessBuilder pb = new ProcessBuilder(
                PYTHON_EXE,
                pythonFile.getAbsolutePath(),
                textToDictate
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        String pythonOutputPath = reader.readLine();
        process.waitFor();

        if (pythonOutputPath == null || pythonOutputPath.startsWith("ERROR")) {
            throw new RuntimeException("Python failed: " + pythonOutputPath);
        }

        System.out.println("Generated audio: " + pythonOutputPath);

        File audioFile = new File(pythonOutputPath);
        if (!audioFile.exists()) {
            throw new RuntimeException("Audio file not found.");
        }

        String fileName = audioFile.getName();

        // 4️⃣ Push to Android
        executeCommand(new String[]{
                ADB_PATH,
                "-d",                // 👈 Target physical device
                "push",
                pythonOutputPath,
                "/sdcard/" + fileName
        });

        // 5️⃣ Play on Android
        executeCommand(new String[]{
                ADB_PATH,
                "-d",                // 👈 Target physical device
                "shell",
                "am",
                "start",
                "-a",
                "android.intent.action.VIEW",
                "-d",
                "file:///sdcard/" + fileName,
                "-t",
                "audio/*"
        });
        // 6️⃣ Delete only the generated WAV (NOT the .py file)
        audioFile.delete();

        System.out.println("Playback started.");
    }


    private static void writePythonScript(File file) throws IOException {

        String pythonCode =
                "import os\n" +
                "import sys\n" +
                "import subprocess\n" +
                "import uuid\n" +
                "import tempfile\n" +
                "\n" +
                "USER_HOME = os.path.expanduser(\"~\")\n" +
                "PIPER_PATH = os.path.join(USER_HOME, \"piper\", \"piper.exe\")\n" +
                "VOICE_MODEL = os.path.join(USER_HOME, \"piper\", \"models\", \"en_US-lessac-medium.onnx\")\n" +
                "\n" +
                "def generate_tts(text):\n" +
                "    if not os.path.exists(PIPER_PATH):\n" +
                "        print(\"ERROR: Piper not found\")\n" +
                "        sys.exit(1)\n" +
                "    if not os.path.exists(VOICE_MODEL):\n" +
                "        print(\"ERROR: Model not found\")\n" +
                "        sys.exit(1)\n" +
                "    file_id = str(uuid.uuid4())\n" +
                "    wav_path = os.path.join(tempfile.gettempdir(), file_id + \".wav\")\n" +
                "    process = subprocess.run([\n" +
                "        PIPER_PATH,\n" +
                "        \"--model\", VOICE_MODEL,\n" +
                "        \"--output_file\", wav_path\n" +
                "    ], input=text.encode(\"utf-8\"), stdout=subprocess.PIPE, stderr=subprocess.PIPE)\n" +
                "    if process.returncode != 0:\n" +
                "        print(\"ERROR:\" + process.stderr.decode())\n" +
                "        sys.exit(1)\n" +
                "    print(wav_path)\n" +
                "\n" +
                "if __name__ == '__main__':\n" +
                "    if len(sys.argv) < 2:\n" +
                "        print(\"ERROR: No text provided\")\n" +
                "        sys.exit(1)\n" +
                "    generate_tts(sys.argv[1])\n";

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pythonCode.getBytes(StandardCharsets.UTF_8));
        }
    }


    private static void executeCommand(String[] command) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }
  } 