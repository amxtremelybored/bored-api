package in.bored.api.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TopicContentCsvExporter {

    // Map<String, List<String>>  — same concept as your Dart contentCategories
    private static final Map<String, List<String>> CONTENT_CATEGORIES = createContentCategories();

    // Optional: Topic → emoji (your contentTopics)
    private static final Map<String, String> CONTENT_TOPICS_ICON = createContentTopicsIcon();

    public static void main(String[] args) {
        try {
            // 1) Export category/topic pairs
            exportCategoryTopicsCsv("topics_by_category.csv");

            // 2) Optional: export topic + emoji (if you want)
            exportTopicEmojiCsv("topics_with_emoji.csv");

            System.out.println("CSV files generated.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private static Map<String, List<String>> createContentCategories() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        // === EXAMPLE: copy from your Dart map, but use Java syntax ===
        // Dart:
        // 'Mythology & Spirituality': [
        //   'Indian Astrology',
        //   'Ancient Philosophy',
        //   ...
        // ],
        map.put("Mythology & Spirituality", Arrays.asList(
                "Indian Astrology",
                "Ancient Philosophy",
                "Ancient India",
                "Ramayana",
                "Mahabharatha",
                "Mythology",
                "Religions of the world",
                "Religious Studies",
                "Ramayana",
                "Lord Krishna",
                "Lord Shiva",
                "Lord Narasimha",
                "Facts about Jesus"
        ));

        map.put("Evolution in the Vedic Age", Arrays.asList(
                "The Composition and Structure of the Four Vedas (Rig, Yajur, Sama, Atharva)",
                "The Evolution of the Varna System (Social Stratification)",
                "The Concept of Rita (Cosmic Order) and Satya (Truth)",
                "The Transition from Pastoralism to Settled Agriculture (Later Vedic Age)",
                "The Significance of the Ashvamedha and Rajasuya Sacrifices",
                "The Role and Status of Women in Early Vedic Society (Sabha and Samiti)",
                "The Philosophical Dialogues of the Upanishads (Atman and Brahman)",
                "The Development of Monarchy and Territorial Kingdoms (Janapadas)",
                "The Major Vedic Deities (Indra, Agni, Varuna) and their Changing Importance",
                "Early Beginnings of Indian Music (Samaveda)",
                "The Advent of Iron Technology and its Impact on Clearing Forests",
                "The Theory of the Aryan Homeland and Migration Debates"
        ));

        map.put("Historical Kings of India", Arrays.asList(
                "Raja Raja Chola I (Chola Dynasty)",
                "Rajendra Chola I (Chola Dynasty)",
                "Krishnadevaraya (Vijayanagara Empire)",
                "Gautamiputra Satakarni (Satavahana Dynasty)",
                "Pulakeshin II (Chalukya Dynasty)",
                "Mahendravarman I (Pallava Dynasty)",
                "Narasimhavarman I (Pallava Dynasty)",
                "Jatavarman Sundara Pandyan (Pandya Dynasty)",
                "Vikramaditya II (Chalukya Dynasty)",
                "Dantidurga (Rashtrakuta Dynasty)",
                "Kanishka (Kushan Dynasty)",
                "Chandragupta II / Vikramaditya",
                "Mihira Bhoja (Gurjara-Pratihara Dynasty)",
                "Raja Porus (Paurava Kingdom)",
                "Bimbisara (Haryanka Dynasty)",
                "Maharaja Ranjit Singh (Sikh Empire)",
                "Maharana Pratap (Mewar Kingdom)",
                "Ashoka the Great (Mauryan Emperor)",
                "Chandragupta Maurya (Founder of the Mauryan Empire)",
                "Samudragupta (Gupta Emperor, \"Napoleon of India\")",
                "Akbar the Great (Mughal Emperor)",
                "Chhatrapati Shivaji Maharaj (Founder of the Maratha Empire)",
                "Raja Raja Chola I (Chola Emperor)",
                "Krishnadevaraya (Vijayanagara Emperor)",
                "Rajendra Chola I (Chola Emperor)",
                "Harshavardhana (Ruler of North India)",
                "Pulakeshin II (Chalukya Ruler)",
                "Prithviraj Chauhan (Rajput King, Chauhan Dynasty)",
                "Ajatashatru (Magadha Empire)",
                "Mahapadma Nanda (Nanda Empire)",
                "Chandrapradyota (Avanti Kingdom)",
                "Udayin (Magadha Empire)",
                "King Kosala (Kosala Kingdom)",
                "Jatavarman Sundara Pandyan I",
                "Maravarman Kulasekara Pandyan I",
                "Nedunjeliyan I (Pandyan - Sangam Age)",
                "Mahendravarman I (Pallava)",
                "Narasimhavarman I (Pallava)",
                "Nandivarman II (Pallava)",
                "Dantivarman (Pallava)"
        ));

        // 👉 CONTINUE: copy the rest of your Dart map exactly the same way:
        //
        // map.put("Rishis of Sanatana Dharma", Arrays.asList(
        //     "Vishwamitra (Composer of the Gayatri Mantra)",
        //     ...
        // ));
        //
        // map.put("The Saptarishis (Seven Great Sages)", Arrays.asList(
        //     "Vasistha",
        //     ...
        // ));
        //
        // ...and so on for ALL keys:
        // 'Architectural Marvels of Pallava Dynasties', 'Languages of India',
        // 'Governance Structure and Hierarchy in India', etc, etc.
        //
        // You can literally mirror your Dart structure:
        //   'Category Name': [ 'topic1', 'topic2', ... ],
        // becomes
        //   map.put("Category Name", Arrays.asList("topic1", "topic2", ...));

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> createContentTopicsIcon() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("Evolution in the Vedic Age", "🌿");
        map.put("Historical Kings of India", "👑");
        map.put("Rishis of Sanatana Dharma", "🧘");
        map.put("The Saptarishis (Seven Great Sages)", "✨");
        map.put("Architectural Marvels of Pallava Dynasties", "🗿");
        map.put("Architectural Marvels of Pandyan Dynasties", "🏛️");
        map.put("Architectural Marvels of the Chola Dynasty", "🔱");
        map.put("Languages of India", "🗣️");
        map.put("Governance Structure and Hierarchy in India", "🇮🇳");
        map.put("Philosophies of Sanatana Dharma", "🧠");
        map.put("Major Sampradāyas in Sanatana Dharma", "🙏");
        map.put("Sacred Texts of Sanatana Dharma", "📜");
        map.put("Kingdoms of Ancient India", "🏰");
        map.put("16 Mahajanapadas of Ancient India", "🗺️");
        map.put("Greatest Cities of Ancient India", "🌆");
        map.put("Indian Divinity", "💫");
        map.put("Places in the Ramayana", "🏞️");
        map.put("Main Characters from the Ramayana", "🏹");
        map.put("Demons (Rakshasas) in Ramayana", "👹");
        map.put("Key Events in the Ramayana", "📖");
        map.put("Places in the Mahabharata", "🏹");
        map.put("Main Characters from the Mahabharata", "⚔️");
        map.put("Key Female Characters from the Mahabharata", "👑");
        map.put("Allies of the Pandavas in the Mahabharata", "🤝");
        map.put("Main Weapons Used in the Mahabharata War", "🛡️");
        map.put("Key Events in the Mahabharata", "📜");
        map.put("Key Figures Who Possessed Powerful Astras in Mahabharatha", "💥");
        map.put("Archaeological Evidences of Ancient India", "🏺");
        map.put("Key Political Events in Indian History", "🗳️");
        map.put("Key Political Events of Ancient North India", "🦁");
        map.put("Key Political Events of Ancient South India", "🐘");
        map.put("Key Battles of Ancient and Early Medieval India", "⚔️");
        map.put("Prominent Freedom Fighters of India", "🕊️");
        map.put("Essential Topics of Indian History", "🕰️");
        map.put("Key Religious Figures of India", "👳");
        map.put("Key Hindu Deities", "🕉️");
        map.put("Key Topics of Sanatana Dharma", "🧘");
        map.put("Divine History of India", "🌟");
        map.put("Ancient Indian and its Wonders", "🛕");
        map.put("India's Key Contributions to the World", "🥇");
        map.put("Inventions and Discoveries from India", "💡");
        map.put("Indian Astrology (Jyotiṣa)", "🪐");
        map.put("India's Engineering Marvels", "🏗️");
        map.put("Destroyed Monuments of India", "🏚️");
        map.put("Mass Religious Congregations of India", "🧑‍🤝‍🧑");
        map.put("Famous Temples of India", "🛕");
        map.put("India in Sports & Games", "🏏");
        map.put("Major Milestones of India in Various Fields", "📈");
        map.put("Spiritual & Mythological Stories of India", "🧚");
        map.put("Indian History", "📚");
        map.put("Indian Sports Achievements", "🏅");
        map.put("Languages & Literature", "🖋️");
        map.put("Everyday Life & Finance", "🏠");
        map.put("Indian Arts", "🎨");
        map.put("Major Indian Government Schemes", "💰");
        map.put("Key Indian Investment Schemes", "🏦");
        map.put("Indian Places Famous for Unique Food Items", "🍽️");
        map.put("Indian Places Famous for Biryani", "🥘");
        map.put("Indian Places Famous for Dosa Varieties", "🥞");
        map.put("India's Famous Culinary Destination", "🌶️");
        map.put("Scary and Intriguing Events from India", "👻");
        map.put("Global Paranormal, Alien, and Urban Legends", "👽");
        map.put("Scary and Mysterious Incidents", "🔪");

        return Collections.unmodifiableMap(map);
    }

    // =========================
    // CSV EXPORT HELPERS
    // =========================

    // 1) category,topic
    private static void exportCategoryTopicsCsv(String fileName) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName, false))) {
            out.println("category,topic");
            for (Map.Entry<String, List<String>> entry : CONTENT_CATEGORIES.entrySet()) {
                String category = entry.getKey();
                for (String topic : entry.getValue()) {
                    out.printf("%s,%s%n",
                            csvEscape(category),
                            csvEscape(topic));
                }
            }
        }
    }

    // 2) topic,emoji (optional)
    private static void exportTopicEmojiCsv(String fileName) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName, false))) {
            out.println("topic,emoji");
            for (Map.Entry<String, String> entry : CONTENT_TOPICS_ICON.entrySet()) {
                out.printf("%s,%s%n",
                        csvEscape(entry.getKey()),
                        csvEscape(entry.getValue()));
            }
        }
    }

    // CSV escaping for commas/quotes
    private static String csvEscape(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
