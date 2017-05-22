import kotlin.Unit;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;

/**
 * Created by hexafraction on 5/21/17.
 */
public class MainGUI {
    public JPanel contentPane;
    private JTextArea result;
    private JPanel topBar;
    private JTextField key;
    private JButton btStart;
    private JButton btStop;
    private JProgressBar progressBar1;
    private SwingWorker<Void, String> worker;

    public MainGUI() {
        btStart.addMouseListener(new MouseAdapter() {


            @Override
            public void mouseClicked(final MouseEvent event) {
                btStop.setEnabled(true);
                btStart.setEnabled(false);
                worker = new SwingWorker<Void, String>() {
                    HashSet<String> seen = new HashSet<>();

                    @Override
                    protected Void doInBackground() throws Exception {
                        Thread th = new Thread(() -> {
                            new KeyTester().tryKeys(key.getText(), s -> {
                                if (!seen.contains(s)) {
                                    publish(s);
                                    seen.add(s);
                                }
                                return Unit.INSTANCE;
                            });
                        });
                        th.start();
                        progressBar1.setIndeterminate(true);
                        try {
                            th.join(); // interrupt to stop
                            progressBar1.setIndeterminate(false);
                        } catch (InterruptedException e) {
                            th.stop(); // deprecated but quick workaround.
                        }
                        btStop.setEnabled(false);
                        btStart.setEnabled(true);
                        return null;
                    }

                    @Override
                    protected void process(final List<String> list) {
                        for (String s : list) {
                            result.append(s);
                            result.append("\n");
                        }
                    }
                };
                worker.execute();
            }
        });
        btStop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                btStop.setEnabled(false);
                worker.cancel(true);
                btStart.setEnabled(true);

                progressBar1.setIndeterminate(false);
            }
        });
    }

}
