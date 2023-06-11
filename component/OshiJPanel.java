package component;

import javax.swing.*;
import java.awt.*;

public class OshiJPanel extends JPanel{
    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    public OshiJPanel(){
        super();
        Dimension maxSize = getMaximumSize();
        if (maxSize != null){
            setSize(maxSize);
        }
        setLayout(new GridBagLayout());
//        setLayout(new BorderLayout());
//        setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
//        msgPanel.add(msgLabel);
//
//        JPanel topPanel = new JPanel(new BorderLayout());
//        topPanel.add(msgPanel, BorderLayout.CENTER);
//        add(topPanel, BorderLayout.NORTH);

//        GridBagConstraints oshiConstraint = new GridBagConstraints();
//        setLayout(new GridBagLayout());
    }
}
