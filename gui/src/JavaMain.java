import javax.swing.*;

/**
 * Created by hexafraction on 5/21/17.
 */
public class JavaMain {
    public static void main(String[] args) {
        JFrame frame = new JFrame("WIF typo repair");
        MainGUI mgui = new MainGUI();
        frame.setContentPane(mgui.contentPane);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        frame.setSize(640, 480);
        frame.setVisible(true);
        mgui.spawnAbout();
    }


}
