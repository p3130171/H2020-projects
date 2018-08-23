/*
 * Created by JFormDesigner on Wed Aug 15 17:15:01 EEST 2018
 */
// Darcula dark theme is used for the application. Find out more: https://github.com/bulenkov/Darcula
// Part of the GUI is created with JFormDesigner. Find out more: https://www.formdev.com/

package search_engine_package;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.bulenkov.darcula.DarculaLaf;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import java.io.*;
import javax.swing.plaf.basic.BasicLookAndFeel;

/**
 * @author p3130171
 */
public class GUI extends JFrame {

	// Variables to be passed to the main method of the application.
	public float titleWeight = (float) 0.5; // Default value.
	public float objectiveWeight = (float) 0.5; // Default value.
	public float callWeight = (float) 30.0; // Default value.
	public String workingDirectory = System.getProperty("user.dir");
	public String queriesFilePath = workingDirectory+"\\Queries.xml"; // Default path.

	public GUI() {

		try { // Update the graphics theme.

			BasicLookAndFeel darcula = new DarculaLaf();
			UIManager.setLookAndFeel(darcula);

		} catch(Exception ignored){}

		initComponents();
	}

	public static void showMessage(String infoMessage, String titleBar)
	{
		JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
	}

	private void SearchButtonPressed(ActionEvent e) {

		// Set the variables taken from the user input to the main class variables.
		SearchEngine.setQueriesFilePath(queriesFilePath);
		SearchEngine.setTitleWeight(titleWeight);
		SearchEngine.setObjectiveWeight(objectiveWeight);
		SearchEngine.setCallWeight(callWeight);

		if (SearchEngine.getReadyToSearch() == true){ // If the index from the main method is completed and ready for search, then proceed.

			SearchEngine.search(); // Call the main searching function of the main class.
			showMessage("The search is completed! The results are saved in \"@RESULTS.txt\".", "Search finished");

		} else { // If the index is still in progress, show the appropriate message.

			showMessage("Indexing is still running! Please try again in a few moments.", "Index is not ready yet");
		}

	}

	private void fileSelectButtonClicked(ActionEvent e) { // If a file is selected.

		JFileChooser fileChooser = new JFileChooser();
		int returnValue = fileChooser.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {

			File selectedFile = fileChooser.getSelectedFile();
			queriesFilePath = selectedFile.getAbsolutePath();

			if (!selectedFile.getName().endsWith(".xml")){ // Not proper file format.

				System.out.println("Wrong file format selected! Only .xml files are accepted.");
				showMessage("Wrong file format selected! Only .xml files are accepted.", "File format error");

			}

			System.out.println("Queries path:" + queriesFilePath);
		}

	}

	private void titleWeightFieldFocusLost(FocusEvent e) {

		String value = titleWeightField.getText(); // Get the value of the user's input.

		if (value!= null){

			try {

				titleWeight = Float.parseFloat(value);
				System.out.println("titleWeight value: " + titleWeight);

			} catch (NumberFormatException n){

				showMessage("Invalid title weight input! Insert a number.", "Invalid weight");

			}

		}

	}

	private void objectiveWeightFieldFocusLost(FocusEvent e) {

		String value = objectiveWeightField.getText(); // Get the value of the user's input.

		if (value!= null){

			try {

				objectiveWeight = Float.parseFloat(value);
				System.out.println("objectiveWeight value: " + objectiveWeight);

			} catch (NumberFormatException l) {

				showMessage("Invalid objective weight input! Insert a number.", "Invalid weight");

			}

		}

	}

	private void categoryWeightFieldFocusLost(FocusEvent e) {

		String value = categoryWeightField.getText(); // Get the value of the user's input.

		if (value!= null){

			try {

				callWeight = Float.parseFloat(value);
				System.out.println("callWeight value: " + callWeight);

			} catch (NumberFormatException m) {

				showMessage("Invalid category weight input! Insert a number.", "Invalid weight");
			}

		}

	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - p3130171
		dialogPane = new JPanel();
		contentPanel = new JPanel();
		fileLabel = new JLabel();
		fileSelectButton = new JButton();
		titleLabel = new JLabel();
		titleWeightField = new JTextField();
		objectiveLabel = new JLabel();
		objectiveWeightField = new JTextField();
		categoryLabel = new JLabel();
		categoryWeightField = new JTextField();
		buttonBar = new JPanel();
		separator = new JSeparator();
		searchButton = new JButton();

		//======== this ========
		setTitle("Lucene Searcher");
		setMinimumSize(new Dimension(420, 300));
		setForeground(Color.black);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		//======== dialogPane ========
		{
			dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
			dialogPane.setLayout(new BorderLayout());

			//======== contentPanel ========
			{
				contentPanel.setPreferredSize(new Dimension(94, 1));
				contentPanel.setLayout(new FormLayout(
					"98dlu:grow, 3dlu:grow, 106dlu:grow",
					"3*(default, $lgap), default"));

				//---- fileLabel ----
				fileLabel.setText("Select queries file:");
				contentPanel.add(fileLabel, CC.xywh(1, 1, 2, 2, CC.CENTER, CC.DEFAULT));

				//---- fileSelectButton ----
				fileSelectButton.setText("Select");
				fileSelectButton.addActionListener(e -> fileSelectButtonClicked(e));
				contentPanel.add(fileSelectButton, CC.xy(3, 1));

				//---- titleLabel ----
				titleLabel.setText("Define title weight:");
				contentPanel.add(titleLabel, CC.xywh(1, 3, 2, 1, CC.CENTER, CC.DEFAULT));

				//---- titleWeightField ----
				titleWeightField.setText("0.5");
				titleWeightField.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						titleWeightFieldFocusLost(e);
					}
				});
				contentPanel.add(titleWeightField, CC.xy(3, 3));

				//---- objectiveLabel ----
				objectiveLabel.setText("Define objective weight:");
				contentPanel.add(objectiveLabel, CC.xywh(1, 5, 2, 1, CC.CENTER, CC.DEFAULT));

				//---- objectiveWeightField ----
				objectiveWeightField.setText("0.5");
				objectiveWeightField.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						objectiveWeightFieldFocusLost(e);
					}
				});
				contentPanel.add(objectiveWeightField, CC.xy(3, 5));

				//---- categoryLabel ----
				categoryLabel.setText("Define category weight:");
				contentPanel.add(categoryLabel, CC.xywh(1, 7, 2, 1, CC.CENTER, CC.DEFAULT));

				//---- categoryWeightField ----
				categoryWeightField.setText("30.0");
				categoryWeightField.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						categoryWeightFieldFocusLost(e);
					}
				});
				contentPanel.add(categoryWeightField, CC.xy(3, 7));
			}
			dialogPane.add(contentPanel, BorderLayout.CENTER);

			//======== buttonBar ========
			{
				buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
				buttonBar.setLayout(new FormLayout(
					"[343dlu,pref]:grow",
					"default, 3dlu, default"));
				buttonBar.add(separator, CC.xywh(1, 1, 1, 2));

				//---- searchButton ----
				searchButton.setText("SEARCH");
				searchButton.addActionListener(e -> SearchButtonPressed(e));
				buttonBar.add(searchButton, CC.xy(1, 3, CC.CENTER, CC.CENTER));
			}
			dialogPane.add(buttonBar, BorderLayout.SOUTH);
		}
		contentPane.add(dialogPane, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - p3130171
	private JPanel dialogPane;
	private JPanel contentPanel;
	private JLabel fileLabel;
	private JButton fileSelectButton;
	private JLabel titleLabel;
	private JTextField titleWeightField;
	private JLabel objectiveLabel;
	private JTextField objectiveWeightField;
	private JLabel categoryLabel;
	private JTextField categoryWeightField;
	private JPanel buttonBar;
	private JSeparator separator;
	private JButton searchButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
