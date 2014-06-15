/*Created by Diedas 15/06/2014
** version 0.1
** GPL 3
** use! all comments welcome!
** This is an Java telnet client. comes in two flavors: java.net.Socket and org.apache.commons.net.telnet.TelnetClient
*/

import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

//Sample usage.

//JavaTelnetClient telnet = new AutomatedTelnetClient(addr, login, password, prompt,10000);
//String result = telnet.sendCommand("iwlist " + iface + " scanning");
//telnet.disconnect();
//System.out.println(result);

public class JavaTelnetClient {

    private TelnetClient telnet;
    private Socket socket;
    private final InputStream in;
    private final PrintStream out;
    private final String login;
    private final String password;
    private final String prompt;
    private long timeout = 1000l;
    private final static boolean debug = false;//set to true for debugging

    //this constructor is for Apache commons TelnetClient socket
    public JavaTelnetClient(String server, String user, String password, String prompt, boolean mode) throws IOException {
        telnet = new TelnetClient();
        telnet.connect(server, 23);
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());
        this.login = user;
        this.password = password;
        this.prompt = prompt;
//        readUntil("Username : ");
//        write(user);
//        readUntil("Password : ");
//        write(password);
        try {
            Thread.sleep(200L);
        } catch (Exception e) {
        }
        write(user);
        try {
            Thread.sleep(200L);
        } catch (Exception e) {
        }
        write(password);
        readUntil(prompt);
    }

    //this constructor is for plain socket. So You do not have to use apache library.
    public JavaTelnetClient(String server, String user, String password, String prompt, Integer timeout) throws IOException {
        socket = new Socket(server, 23);
        in = socket.getInputStream();
        out = new PrintStream(socket.getOutputStream());
        this.login = user;
        this.password = password;
        this.prompt = prompt;
        if (timeout != null)
            this.timeout = timeout;
//        readUntil(loginSTR);
//        write(user);
//        readUntil(passwdSTR);
//        write(password);
        try {
            Thread.sleep(200L);
        } catch (Exception e) {
        }
        write(user);
        try {
            Thread.sleep(200L);
        } catch (Exception e) {
        }
        write(password);
        readUntil(prompt);
    }

    public String readUntil(String pattern) throws IOException {
        long lastTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = -1;
            byte[] text;
            if (in.available() > 0) {
                c = in.read(text = new byte[in.available()]);
                sb.append(new String(text));
            }
            long now = System.currentTimeMillis();
            if (c != -1) {
                lastTime = now;
            }
            if (now - lastTime > timeout) {
                break;
            }
            if (sb.toString().contains(pattern)) {
                return sb.toString();
            }
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }

    //just sends text to server
    public void print(String value) {
        try {
            out.println(value + ";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //this method writes to server, but waits no prompt.
    public void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //sends command and receives result
    public String sendCommand(String command) {
        try {
            write(command);
            String until = readUntil(prompt);
            if (debug)
                System.out.println("\n command -> \n" + command + " \n ouptut-> \n" + until);
            return until;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //closes a this client. you may want to send command "exit" beforehand
    public void disconnect() {
        try {
            if (socket != null)
                socket.close();
            if (telnet != null)
                telnet.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
