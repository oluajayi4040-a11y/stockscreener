package stockscreener.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicSP500Loader {

    private static final String SP500_URL =
            "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies";

    public List<String> getSymbols() {

        List<String> symbols = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(SP500_URL).get();

            // First table on the page contains the S&P 500 list
            Elements rows = doc.select("table.wikitable tbody tr");

            rows.forEach(row -> {
                Elements cols = row.select("td");
                if (cols.size() > 0) {
                    String symbol = cols.get(0).text().trim();
                    symbols.add(symbol);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return symbols;
    }
}
