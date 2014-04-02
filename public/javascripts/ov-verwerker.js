function getCurrentPage() {
  return parseInt($(".paginering li").has("em").text().trim());
}

var weekdays = ['zondag', 'maandag', 'dinsdag', 'woensdag', 'donderdag', 'vrijdag', 'zaterdag'];
var pageToCheck = getCurrentPage();
var clickQueue = [];

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
  var currentPage = getCurrentPage();
  console.log("checkThisPage: " + currentPage + ", " + pageToCheck);
  if (currentPage == pageToCheck) {
    $("tr").has("td:contains('Check-uit')").each(function(index) {
      var date = getRowDate(this);
      var workingDay = isWorkingDay(date.getDay());
      $(this).find(":checkbox").each(function() {
        if (workingDay != $(this).prop("checked")) {
          clickQueue.push(index);
          console.log("Pushed to queue: " + clickQueue);
        }
      });
      $(this).css("background-color", workingDay ? "lime" : "red");
    });
    setTimeout(clearClickQueue, 500);
  } else {
    setInterval(addWeekdays, 500);
  }
}

function clearClickQueue() {
  console.log("Clearing queue: " + clickQueue);
  if (clickQueue.length > 0) {
    $($("tr").has("td:contains('Check-uit')")[clickQueue.shift()]).find(":checkbox").click();
    setTimeout(clearClickQueue, 1000);
  } else {
    moveToNextPage();
  }
}

function moveToNextPage() {
  console.log("Moving to next page");
  pageToCheck++;
  $(".volgende a").click();
  setTimeout(checkThisPage, 2000);
}

checkThisPage();