package com.github.danfickle.flyingsaucer.bootstrap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import com.github.danfickle.flyingsaucer.swing.XHTMLPanel;


public class BsButton extends JButton
{
	private static final long serialVersionUID = 1L;
	private final XHTMLPanel _pnl;
	
	public BsButton(String clz, String text) 
	{
		super(text);
		_pnl = BsUtil.setup(String.format("<button class=\"%s\">%s</button>", clz, text), super.getPreferredSize());

		this.add(_pnl);
		this.setBackground(BsUtil.TRANSPARENT);
		this.setForeground(BsUtil.TRANSPARENT);
		this.setBorder(BorderFactory.createEmptyBorder());
	}
}
