function getCurrentPage() {
  return parseInt($(".paginering li").has("em").text().trim());
}

var weekdays = ['zondag', 'maandag', 'dinsdag', 'woensdag', 'donderdag', 'vrijdag', 'zaterdag'];
var pageToCheck = getCurrentPage();

function getRowDate(row) {
  var dateStr = $(row).find("td").first().text().trim();
  dateStr = dateStr.substr(6, 4) + "-" + dateStr.substr(3, 2) + "-" + dateStr.substr(0, 2);
  return new Date(Date.parse(dateStr));
}

function isWorkingDay(dayOfWeek) {
  return dayOfWeek >= 2 && dayOfWeek <= 4;
}

function addWeekdays() {
  $("tr").has("td").not(":has(td.weekday)").each(function() {
    var date = getRowDate(this);
    $(this).append('<td class="weekday">' + weekdays[date.getDay()] + "</td>");
  });
}

function checkThisPage() {
  if (getCurrentPage() == pageToCheck) {
    $("tr").has("td:contains('Check-uit')").each(function() {
      var date = getRowDate(this);
      var workingDay = isWorkingDay(date.getDay());
      $(this).find(":checkbox").each(function() {
        if (workingDay != $(this).prop("checked")) {
          $(this).click();
        }
      });
      $(this).css("background-color", $(this).find(":checkbox").prop("checked") ? "lime" : "red");
    });
    setTimeout(moveToNextPage, 2000);
  } else {
    setInterval(addWeekdays, 1500);
  }
}

function moveToNextPage() {
  pageToCheck++;
  $(".volgende a").click();
  setTimeout(checkThisPage, 2000);
}

checkThisPage();