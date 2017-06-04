package edu.goergetown.bioasq.core.model.mesh;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/22/2017.
 */
public class MeSHUtils {
    private static ArrayList<String> listOfCheckTags = null;
    private static ArrayList<String> countries = null;

    public static ArrayList<String> getCheckTagsMeSH(boolean createNew) {
        if (createNew) {
            return createNewList();
        } else {
            return getCheckTagsMeSH();
        }
    }

    public static ArrayList<String> getCheckTagsMeSH() {
        if (listOfCheckTags == null) {
            listOfCheckTags = createNewList();
        }

        return listOfCheckTags;
    }

    private static ArrayList<String> createNewList() {
        ArrayList<String> list = new ArrayList<>();

        list.add("Adolescent");
        list.add("Adult");
        list.add("Young Adult");
        list.add("Aged");
        list.add("Aged, 80 and over");
        list.add("Humans");
        list.add("Infant");
        list.add("Child");
        list.add("Male");
        list.add("Child, Preschool");
        list.add("Middle Aged");
        list.add("Female");
        list.add("Swine");
        list.add("Animals");
        list.add("Adult");

        list.addAll(getCountriesList());

        return list;
    }

    public static ArrayList<String> getCountriesList() {
        if (countries == null) {
            countries = new ArrayList<>();

            countries.add("Afghanistan");
            countries.add("Albania");
            countries.add("Algeria");
            countries.add("Andorra");
            countries.add("Angola");
            countries.add("Antigua and Barbuda");
            countries.add("Argentina");
            countries.add("Armenia");
            countries.add("Aruba");
            countries.add("Australia");
            countries.add("Austria");
            countries.add("Azerbaijan");
            countries.add("Bahamas, The");
            countries.add("Bahrain");
            countries.add("Bangladesh");
            countries.add("Barbados");
            countries.add("Belarus");
            countries.add("Belgium");
            countries.add("Belize");
            countries.add("Benin");
            countries.add("Bhutan");
            countries.add("Bolivia");
            countries.add("Bosnia and Herzegovina");
            countries.add("Botswana");
            countries.add("Brazil");
            countries.add("Brunei");
            countries.add("Bulgaria");
            countries.add("Burkina Faso");
            countries.add("Burma");
            countries.add("Burundi");
            countries.add("Cambodia");
            countries.add("Cameroon");
            countries.add("Canada");
            countries.add("Cabo Verde");
            countries.add("Central African Republic");
            countries.add("Chad");
            countries.add("Chile");
            countries.add("China");
            countries.add("Colombia");
            countries.add("Comoros");
            countries.add("Congo, Democratic Republic of the");
            countries.add("Congo, Republic of the");
            countries.add("Costa Rica");
            countries.add("Cote d'Ivoire");
            countries.add("Croatia");
            countries.add("Cuba");
            countries.add("Curacao");
            countries.add("Cyprus");
            countries.add("Czechia");
            countries.add("Denmark");
            countries.add("Djibouti");
            countries.add("Dominica");
            countries.add("Dominican Republic");
            countries.add("East Timor (see Timor-Leste)");
            countries.add("Ecuador");
            countries.add("Egypt");
            countries.add("El Salvador");
            countries.add("Equatorial Guinea");
            countries.add("Eritrea");
            countries.add("Estonia");
            countries.add("Ethiopia");
            countries.add("Fiji");
            countries.add("Finland");
            countries.add("France");
            countries.add("Gabon");
            countries.add("Gambia, The");
            countries.add("Georgia");
            countries.add("Germany");
            countries.add("Ghana");
            countries.add("Greece");
            countries.add("Grenada");
            countries.add("Guatemala");
            countries.add("Guinea");
            countries.add("Guinea-Bissau");
            countries.add("Guyana");
            countries.add("Haiti");
            countries.add("Holy See");
            countries.add("Honduras");
            countries.add("Hong Kong");
            countries.add("Hungary");
            countries.add("Iceland");
            countries.add("India");
            countries.add("Indonesia");
            countries.add("Iran");
            countries.add("Iraq");
            countries.add("Ireland");
            countries.add("Israel");
            countries.add("Italy");
            countries.add("Jamaica");
            countries.add("Japan");
            countries.add("Jordan");
            countries.add("Kazakhstan");
            countries.add("Kenya");
            countries.add("Kiribati");
            countries.add("Korea, North");
            countries.add("Korea, South");
            countries.add("Kosovo");
            countries.add("Kuwait");
            countries.add("Kyrgyzstan");
            countries.add("Laos");
            countries.add("Latvia");
            countries.add("Lebanon");
            countries.add("Lesotho");
            countries.add("Liberia");
            countries.add("Libya");
            countries.add("Liechtenstein");
            countries.add("Lithuania");
            countries.add("Luxembourg");
            countries.add("Macau");
            countries.add("Macedonia");
            countries.add("Madagascar");
            countries.add("Malawi");
            countries.add("Malaysia");
            countries.add("Maldives");
            countries.add("Mali");
            countries.add("Malta");
            countries.add("Marshall Islands");
            countries.add("Mauritania");
            countries.add("Mauritius");
            countries.add("Mexico");
            countries.add("Micronesia");
            countries.add("Moldova");
            countries.add("Monaco");
            countries.add("Mongolia");
            countries.add("Montenegro");
            countries.add("Morocco");
            countries.add("Mozambique");
            countries.add("Namibia");
            countries.add("Nauru");
            countries.add("Nepal");
            countries.add("Netherlands");
            countries.add("New Zealand");
            countries.add("Nicaragua");
            countries.add("Niger");
            countries.add("Nigeria");
            countries.add("North Korea");
            countries.add("Norway");
            countries.add("Oman");
            countries.add("Pakistan");
            countries.add("Palau");
            countries.add("Palestinian Territories");
            countries.add("Panama");
            countries.add("Papua New Guinea");
            countries.add("Paraguay");
            countries.add("Peru");
            countries.add("Philippines");
            countries.add("Poland");
            countries.add("Portugal");
            countries.add("Qatar");
            countries.add("Romania");
            countries.add("Russia");
            countries.add("Rwanda");
            countries.add("Saint Kitts and Nevis");
            countries.add("Saint Lucia");
            countries.add("Saint Vincent and the Grenadines");
            countries.add("Samoa");
            countries.add("San Marino");
            countries.add("Sao Tome and Principe");
            countries.add("Saudi Arabia");
            countries.add("Senegal");
            countries.add("Serbia");
            countries.add("Seychelles");
            countries.add("Sierra Leone");
            countries.add("Singapore");
            countries.add("Sint Maarten");
            countries.add("Slovakia");
            countries.add("Slovenia");
            countries.add("Solomon Islands");
            countries.add("Somalia");
            countries.add("South Africa");
            countries.add("South Korea");
            countries.add("South Sudan");
            countries.add("Spain");
            countries.add("Sri Lanka");
            countries.add("Sudan");
            countries.add("Suriname");
            countries.add("Swaziland");
            countries.add("Sweden");
            countries.add("Switzerland");
            countries.add("Syria");
            countries.add("Taiwan");
            countries.add("Tajikistan");
            countries.add("Tanzania");
            countries.add("Thailand");
            countries.add("Timor-Leste");
            countries.add("Togo");
            countries.add("Tonga");
            countries.add("Trinidad and Tobago");
            countries.add("Tunisia");
            countries.add("Turkey");
            countries.add("Turkmenistan");
            countries.add("Tuvalu");
            countries.add("Uganda");
            countries.add("Ukraine");
            countries.add("United Arab Emirates");
            countries.add("United Kingdom");
            countries.add("Uruguay");
            countries.add("Uzbekistan");
            countries.add("Vanuatu");
            countries.add("Venezuela");
            countries.add("Vietnam");
            countries.add("Yemen");
            countries.add("Zambia");
            countries.add("Zimbabwe");
        }

        return countries;
    }
}
