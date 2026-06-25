package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class XmlUtils {

    private static final String GAME_MOVES_XML_FILE = "xml/gameMoves.xml";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String TILE_TYPE = "TileType";

    private XmlUtils() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("HttpUrlsUsage")
    private static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }

    private static Document loadOrCreateDocument(DocumentBuilder db, File xmlFile) throws IOException {
        if (xmlFile.exists() && xmlFile.length() > 0) {
            try {
                return db.parse(xmlFile);
            } catch (SAXException e) {
                log.error("XML file is corrupted. Overwriting with a new document. Error: {}", e.getMessage());
                return createNewDocument(db);
            }
        }
        return createNewDocument(db);
    }

    private static Document createNewDocument(DocumentBuilder db) {
        Document doc = db.newDocument();
        Element root = doc.createElement("GameMoves");
        doc.appendChild(root);
        return doc;
    }

    public static synchronized void appendGameMove(GameMove move) {
        File xmlFile = new File(GAME_MOVES_XML_FILE);

        try {
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = loadOrCreateDocument(db, xmlFile);

            Element rootElement = doc.getDocumentElement();
            Element newMoveElement = createGameMoveElement(doc, move);
            rootElement.appendChild(newMoveElement);

            writeDocument(doc);

        } catch (ParserConfigurationException | IOException | TransformerException e) {
            throw new FxmlLoadException("Error appending game move to XML", e);
        }
    }

    public static synchronized List<GameMove> readGameMoves() {
        List<GameMove> moves = new ArrayList<>();
        File xmlFile = new File(GAME_MOVES_XML_FILE);
        if (!xmlFile.exists()) {
            return moves;
        }

        try {
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("GameMove");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;
                    String playerName = element.getElementsByTagName("PlayerName").item(0).getTextContent();
                    ActionType actionType = ActionType.valueOf(element.getElementsByTagName("ActionType").item(0).getTextContent());
                    String details = element.getElementsByTagName("Details").item(0).getTextContent();
                    String message = element.getElementsByTagName("Message").item(0).getTextContent();
                    LocalDateTime timestamp = LocalDateTime.parse(
                            element.getElementsByTagName("Timestamp").item(0).getTextContent(),
                            FORMATTER
                    );

                    Integer row = null;
                    if (element.getElementsByTagName("Row").getLength() > 0) {
                        row = Integer.parseInt(element.getElementsByTagName("Row").item(0).getTextContent());
                    }

                    Integer col = null;
                    if (element.getElementsByTagName("Col").getLength() > 0) {
                        col = Integer.parseInt(element.getElementsByTagName("Col").item(0).getTextContent());
                    }

                    TileType tileType = null;
                    if (element.getElementsByTagName(TILE_TYPE).getLength() > 0) {
                        tileType = TileType.valueOf(element.getElementsByTagName(TILE_TYPE).item(0).getTextContent());
                    }

                    GameMove move = new GameMove(playerName, actionType, details, message, row, col, tileType, timestamp);
                    moves.add(move);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new FxmlLoadException("Error reading game moves from XML", e);
        }
        return moves;
    }

    public static synchronized void clearGameMoves() {
        Path path = Paths.get(GAME_MOVES_XML_FILE);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FxmlLoadException("Failed to delete game moves XML file", e);
        }
    }

    private static void writeDocument(Document doc) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = transformerFactory.newTransformer();

        DOMSource source = new DOMSource(doc);

        try (FileWriter writer = new FileWriter(XmlUtils.GAME_MOVES_XML_FILE)) {
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        }
    }

    private static Element createGameMoveElement(Document doc, GameMove move) {
        Element gameMoveElement = doc.createElement("GameMove");

        gameMoveElement.appendChild(createElement(doc, "PlayerName", move.playerName()));
        gameMoveElement.appendChild(createElement(doc, "ActionType", move.actionType().name()));
        gameMoveElement.appendChild(createElement(doc, "Details", move.details()));
        gameMoveElement.appendChild(createElement(doc, "Message", move.message()));
        gameMoveElement.appendChild(createElement(doc, "Timestamp", move.timestamp().format(FORMATTER)));

        if (move.row() != null) {
            gameMoveElement.appendChild(createElement(doc, "Row", move.row().toString()));
        }
        if (move.col() != null) {
            gameMoveElement.appendChild(createElement(doc, "Col", move.col().toString()));
        }
        if (move.tileType() != null) {
            gameMoveElement.appendChild(createElement(doc, TILE_TYPE, move.tileType().name()));
        }

        return gameMoveElement;
    }

    private static Element createElement(Document doc, String name, String value) {
        Element el = doc.createElement(name);
        el.appendChild(doc.createTextNode(value));
        return el;
    }
}