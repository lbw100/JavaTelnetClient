import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.StringTokenizer;
import java.util.Vector;
/* exaple for the project
 * used to configure the WAN interface on the modem
 */
public class TelnetDeviceConfigurator{
    private String addr;
    private String password;
    private String login;
    private BufferedReader stdOut;
    private int exitValue;
    private String prompt = ">";

    public void rebootModem() throws IOException {
        JavaTelnetClient telnet = new JavaTelnetClient(addr, login, password, prompt, 10000);
        System.out.println( "" + telnet.sendCommand("save"));
        System.out.println( "" + telnet.sendCommand("reboot"));
        telnet.disconnect();
    }

    //setup access point
    public void setupWLCTLAP(String ssid, String passw) throws IOException {
        JavaTelnetClient telnet = new JavaTelnetClient(addr, login, password, prompt, 10000);
        System.out.println( "~~~" + telnet.sendCommand("wlan config --enable 0"));
        System.out.println( "~~~" + telnet.sendCommand("wlan config --ssid \"" + ssid + "\""));
        System.out.println( "~~~" + telnet.sendCommand("wlan security psk --wpaenc tkip+aes"));
        System.out.println( "~~~" + telnet.sendCommand("wlan security psk --rekey 20\n"));
        System.out.println( "~~~" + telnet.sendCommand("wlan security psk --pskey \"" + passw + "\""));
        System.out.println( "~~~" + telnet.sendCommand("wlan config --enable 1"));
        System.out.println( "~~~" + telnet.sendCommand("save"));
        System.out.println( "~~~" + telnet.sendCommand("reboot"));
        try{Thread.sleep(200l);}catch(Exception e){}
        telnet.disconnect();
    }

    //dismantle access point
    public void deactivateWLCTLAP() throws IOException {
        JavaTelnetClient telnet = new JavaTelnetClient(addr, login, password, prompt, 10000);
        System.out.println( "" + telnet.sendCommand("wlan config --enable 0"));
        System.out.println( "" + telnet.sendCommand("save"));
        System.out.println( "" + telnet.sendCommand("exit"));
        telnet.disconnect();
    }

    public Boolean resetDataCounters() throws Exception {
        boolean sent_data;
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write("xdslctl info  --pbParams --reset\n".getBytes());
            outputStream.flush();
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            throw new Exception(e.getMessage());
        }
        return sent_data;
    }

    public boolean update(String address, String path_frm_vdsl) throws Exception {
        boolean sent_data = false;
        String result = "";
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write(("tftp -g " + address + " -f " + path_frm_vdsl + " \n").getBytes());
            outputStream.flush();
            Thread.sleep(1000);
            for (int i = 0; i < 20; i++) {
                result = consumeInput(1000, inputStream);
                if (result.contains("Flashing")) {
                    sent_data = true;
                    i = 50;
                }
                Thread.sleep(1000);
            }
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
        return sent_data;
    }
 
    public void removeVlan() throws Exception {
        String result = "";
        boolean sent_data = false;
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write("ifconfig\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            // get the existing vpi and vci to erase the interface
            stdOut = new BufferedReader(new StringReader(consumeInput(500, inputStream)), 1024);
            String the_line = "";
            while ((the_line = stdOut.readLine()) != null) {
                if (the_line.contains("ptm0.")) {
                    StringTokenizer st = new StringTokenizer(the_line, " ");
                    result = st.nextToken();
                    break;
                }
            }
            Thread.sleep(300);
            if (!result.equals("")) {
                outputStream.write("sh\n".getBytes());
                outputStream.flush();
                Thread.sleep(500);
                outputStream.write(("vconfig rem " + result + "\n").getBytes());
                outputStream.flush();
                Thread.sleep(500);
                outputStream.write("exit\n".getBytes());
                outputStream.flush();
                Thread.sleep(300);
            }
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
    }

    public void connect5(int vlanID, boolean inEmulated) throws Exception {
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(100);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(100);
            outputStream.write("sh\n".getBytes());
            outputStream.flush();
            Thread.sleep(100);
            outputStream.flush();
            Thread.sleep(500);
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(500);
            if (inEmulated) {
                outputStream.flush();
                Thread.sleep(500);
            }
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(500);
            s.close();
            exitValue = 0;
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            throw new Exception(e.getMessage());
        }
    }

    public Boolean connect6(String user, String pass, String ipInterface, String netmask) throws Exception {
        boolean sent_data = false;
        Socket s = null;
        try {
            removeVlan();
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
//            outputStream.write(("lan config --ipaddr primary 201.20.2.1 255.255.255.0  \n").getBytes());
//            outputStream.flush();
//            outputStream.write(("lan config --ipaddr secondary " + ipInterface + " " + netmask + " \n").getBytes());
//            outputStream.flush();
            outputStream.write(("lan config --ipaddr secondary 201.20.2.1 255.255.255.0  \n").getBytes());
            //outputStream.flush();
            outputStream.write(("lan config --ipaddr primary " + ipInterface + " " + netmask + " \n").getBytes());
            //outputStream.flush();
            outputStream.write("save\n".getBytes());
            outputStream.flush();
            Thread.sleep(500);
            //	outputStream.write("reboot\n".getBytes());
            //	Thread.sleep(300);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
        return sent_data;
    }

    public Boolean disableWLAN() throws Exception {
        boolean sent_data = false;
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(150);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(150);
            outputStream.write(("wlan config --enable 0 \n").getBytes());
            outputStream.flush();
            Thread.sleep(50);
            outputStream.write("save\n".getBytes());
            outputStream.flush();
            Thread.sleep(50);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        } finally {
            return sent_data;
        }
    }

    public String getWanIpAddress() throws Exception {
        boolean sent_data = false;
        String result = "";
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            consumeInput(200, inputStream);
            outputStream.write(("wan show\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            // get the existing vpi and vci to erase the interface
            stdOut = new BufferedReader(new StringReader(consumeInput(200, inputStream)), 1024);
            String the_line = "";
            String aux = "";
            while ((the_line = stdOut.readLine()) != null) {
                if (!the_line.trim().startsWith(">") && !the_line.trim().startsWith("VCC") && !the_line.trim().startsWith("ID"))
                    aux = the_line.trim();
            }
            if (!aux.contains("wan show") && !aux.contains("VCC") && !aux.contains("ID")) {
                Vector<String> aa = new Vector<String>();
                StringTokenizer st = new StringTokenizer(aux, " \t");
                while (st.hasMoreTokens()) {
                    aa.add(st.nextToken());
                }
                if (aa.size() >= 2)
                    result = aa.get(aa.size() - 2) + " " + aa.get(aa.size() - 1);
            }
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
        return result;
    }

    public String getWanIpAddress2() throws Exception {
        boolean sent_data = false;
        String result = "";
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            consumeInput(200, inputStream);
            outputStream.write(("wan show\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            // get the existing vpi and vci to erase the interface
            String res = consumeInput(200, inputStream);
            stdOut = new BufferedReader(new StringReader(res), 1024);
            String the_line = "";
            String aux = "";
            while ((the_line = stdOut.readLine()) != null) {
                if (!the_line.trim().startsWith(">") && !the_line.trim().startsWith("VCC") && !the_line.trim().startsWith("ID")){
                    aux = the_line.trim();
                    //break;
                }
            }
            if (!aux.contains("wan show") && !aux.contains("VCC") && !aux.contains("ID")) {
                Vector<String> aa = new Vector<String>();
                StringTokenizer st = new StringTokenizer(aux, " \t");
                while (st.hasMoreTokens()) {
                    aa.add(st.nextToken());
                }
                if (aa.size() >= 2)
                    result = aa.get(aa.size() - 2) + " " + "NONE";
            }
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
        return result;
    }

    public Boolean sendIpTables(String port_number) throws Exception {
        boolean sent_data = false;
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write(("sh\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write(("iptables -t nat -A PREROUTING -p tcp -j DNAT --to-destination 201.20.2.2 --dport " + port_number + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            Thread.sleep(300);
            exitValue = 0;
            sent_data = true;
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            sent_data = false;
            throw new Exception(e.getMessage());
        }
        return sent_data;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLogin(String login) {
        this.login = login;
    }
    /**
     * Consumes all input that appears within a certain timeout. This is useful
     * to get ride of post-login babble.
     *
     * @throws IOException
     */
    private String consumeInput(long timeout, InputStream in) throws IOException {
        boolean doContinue = true;
        long lastTime = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        while (doContinue) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return sb.toString();
            }
            int c = -1;
            while (in.available() > 0) {
                c = in.read();
                sb.append((char) c);
            }
            long now = System.currentTimeMillis();
            if (now - lastTime > timeout)
                doContinue = false; // throw new RuntimeException("TIMEOUT");
            if (c != -1) {
                lastTime = now;
            } // else System.err.println("No reads for
            // "+TimeInterval.describe(now-lastTime));
        }
        return sb.toString();
    }

    public String getFirmwareVersionVdsl() throws Exception {
        String version = "";
        String[] words;
        Socket s = null;
        try {
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(addr), 23);
            s = new Socket();
            s.connect(socketAddress, 1000);
            InputStream inputStream = s.getInputStream();
            OutputStream outputStream = s.getOutputStream();
            outputStream.write((login + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write((password + "\n").getBytes());
            outputStream.flush();
            Thread.sleep(300);
            outputStream.write(("adsl --version" + "\n").getBytes());
            outputStream.flush();
            version = consumeInput(500, inputStream);
            exitValue = 0;
            words = version.split("\\s+");
            int i2 = 0;
            for (int i = 0; i < words.length; i++) {
                if (words[i].contains("version") & !words[i].contains("--")) {
                    i2 = i;
                }
            }
            version = words[i2 + 2];
            s.close();
        } catch (Exception e) {
            exitValue = -1;
            try {
                s.close();
            } catch (Exception e1) {
            }
            version = "false";
            throw new Exception(e.getMessage());
        }
        return version;
    }

}
