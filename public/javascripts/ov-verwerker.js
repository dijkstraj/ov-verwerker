var ovVerwerker = (function() {
  var weekdays = ['zondag', 'maandag', 'dinsdag', 'woensdag', 'donderdag', 'vrijdag', 'zaterdag'];
  var pageToCheck = false;
  var clickQueue = [];

  var weekdaysInputs = function() {
    var result = $('<p></p>');
    for (var i = 1; i <= 7; i++) {
      var d = i % weekdays.length;
      result.append($('<label><input type="checkbox" id="weekday' + d + '" value="' + d + '">' + weekdays[d] + '</label>'));
    }
    result.append($("<div></div>").css({clear: "both"}));
    return result;
  };

  var $options = function() { 
    return $('<div id="ov-options" class="box aside"></div>')
      .append($('<div class="head"></div>'))
      .append($('<div class="body"></div>')
          .append($("<h2>Declareren</h2>"))
          .append(weekdaysInputs())
          .append($('<p></p>')
            .append($('<button class="moc"></button>')
              .append($('<span>Start</span>'))
              .click(startWork))))
      .append($('<div class="foot"></div>'));
  };

  var init = function() {
    $(".secundair").append($options());
  },

  getCurrentPage = function() {
    return parseInt($(".paginering li").has("em").text().trim());
  },

  getRowDate = function(row) {
    var dateStr = $(row).find("td").first().text().trim();
    dateStr = dateStr.substr(6, 4) + "-" + dateStr.substr(3, 2) + "-" + dateStr.substr(0, 2);
    return new Date(Date.parse(dateStr));
  },
  
  isWorkingDay = function(dayOfWeek) {
    return $('#weekday' + dayOfWeek).prop('checked');
  },

  addWeekdays = function() {
    $("tr").has("td").not(":has(td.weekday)").each(function() {
      var date = getRowDate(this);
      $(this).append('<td class="weekday">' + weekdays[date.getDay()] + "</td>");
    });
  },

  startWork = function() {
    pageToCheck = getCurrentPage();
    checkThisPage();
  },

  checkThisPage = function() {
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
  },

  clearClickQueue = function() {
    console.log("Clearing queue: " + clickQueue);
    if (clickQueue.length > 0) {
      $($("tr").has("td:contains('Check-uit')")[clickQueue.shift()]).find(":checkbox").click();
      setTimeout(clearClickQueue, 1000);
    } else {
      moveToNextPage();
    }
  },

  moveToNextPage = function() {
    console.log("Moving to next page");
    pageToCheck++;
    $(".volgende a").click();
    setTimeout(checkThisPage, 2000);
  };
  
  return {
    go: init
  };
  
})();

ovVerwerker.go();