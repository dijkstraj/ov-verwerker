function getCurrentPage() {
  return parseInt($(".paginering li").has("em").text().trim());
}

var weekdays = ['zondag', 'maandag', 'dinsdag', 'woensdag', 'donderdag', 'vrijdag', 'zaterdag'];
var pageToCheck = getCurrentPage();

function checkThisPage() {
  if (getCurrentPage() == pageToCheck) {
    $("tr").has("td:contains('Check-uit')").css("background-color", "red").each(function() {
      var dateStr = $(this).find("td").first().text().trim();
      dateStr = dateStr.substr(6, 4) + "-" + dateStr.substr(3, 2) + "-" + dateStr.substr(0, 2);
      var date = new Date(Date.parse(dateStr));
      var dayOfWeek = date.getDay();
      var workingDay = dayOfWeek >= 2 && dayOfWeek <= 4;
      $(this).append("<td>" + weekdays[dayOfWeek] + "</td>");
      $(this).find(":checkbox").each(function() {
        if (workingDay != $(this).prop("checked")) {
          $(this).click();
        }
      });
    });
    
    pageToCheck++;
    $(".volgende a").click();
    setTimeout(checkThisPage, 1000);
  }
}

checkThisPage();