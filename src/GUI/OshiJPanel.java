package GUI;

import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Parent class combining code common to the other panels.
 */
public class OshiJPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    public OshiJPanel() {
        super();
        Dimension maxSize = getMaximumSize();
        if (maxSize != null) {
            setSize(maxSize);
        }
        setLayout(new GridBagLayout());
    }

    public static void print(Object... content)
    {
        for (Object object: content)
        {
            System.out.println(object);
        }
    }
}
