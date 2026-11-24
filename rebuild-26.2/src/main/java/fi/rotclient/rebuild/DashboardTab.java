package fi.rotclient.rebuild;

final class DashboardTab {
    final String id;
    DashboardScreen.Page page = DashboardScreen.Page.OVERVIEW;
    Category category;
    String search = "";

    DashboardTab(String id) {
        this.id = id;
    }

    String title() {
        return category != null && page == DashboardScreen.Page.MODULES ? category.title() : page.title;
    }
}
