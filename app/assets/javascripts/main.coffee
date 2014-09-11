###global define, angular ###

require([ 'angular', 'angular-animate', 'underscorejs', 'bootstrap',
    'ui-bootstrap', 'ui-bootstrap-tpls' ], (angular) ->
  'use strict';

  module = angular.module('ov-verwerker', [ 'ngAnimate', 'ui.bootstrap' ])

  module.directive('focusOn', ->
    (scope, elem, attr) ->
      scope.$on('focusOn',
        (e, name) ->
          elem[0].focus() if (name == attr.focusOn)
      )
  )    

  module.factory('focus', [ '$rootScope', '$timeout',
      ($rootScope, $timeout) ->
        (name) ->
          $timeout(->
            $rootScope.$broadcast('focusOn', name)
          )
  ]);

  module.controller('MainCtrl', [ '$scope', '$window', '$http', 'focus',
    ($scope, $window, $http, focus) ->
      $scope.loggedIn = false
      $scope.username = $window.localStorage.username || ''
      $scope.password = $window.localStorage.password || ''
      $scope.periods = []
      $scope.transactions = []
      $scope.days = {}
      $scope.pdfs = {}
      $scope.tasks = {}
      $scope.globalTasks = {}
      $scope.weekDays = [
        'zondag'
        'maandag'
        'dinsdag'
        'woensdag'
        'donderdag'
        'vrijdag'
        'zaterdag'
      ]
      
      selectedDay = (day) ->
        $scope.rules.destinations[day.destination] or ($scope.rules.days[day.date.getDay()] and $scope.rules.destinations[day.destination] != false)

      $scope.numberOfSelectedDays = ->
        if $scope.rules?
          _($scope.rules.days).filter((d) -> d).length
        else
          0

      $scope.numberOfDestinations = ->
        if $scope.rules?
          _($scope.rules.destinations).keys().length
        else
          0

      updateDays = ->
        days = _($scope.transactions).groupBy((tx) -> tx.date)
        $scope.days = _(days).map (transactions, date) ->
          console.log(date, transactions)
          txs = _(transactions).sortBy('time')
          destination = _(txs).reduce(((memo, tx) ->
            if not memo.destination?
              memo.destination = tx.out
            timeDiff = parseInt(tx.time) - parseInt(memo.time)
            if timeDiff > memo.timeDiff
              memo.destination = memo.out
              memo.timeDiff = timeDiff
            memo.time = tx.time
            memo.out = tx.out
            memo
          ), {timeDiff: 0})
          result =
            date: new Date(date)
            transactions: txs
            destination: destination?.destination
            total: _(txs).reduce(((memo, tx) -> memo + tx.price), 0)
          result.selected = selectedDay(result)
          console.log(result)
          result
        console.log($scope.days)
      
      $scope.login = ->
        $scope.loggingIn = true
        $scope.globalTasks['login'] =
          name: 'Login controleren op ov-chipkaart.nl'
          progress: 100
        socket.send(
          angular.toJson(
            username: $scope.username
            password: $scope.password
          )
        )
        
      periodKey = ->
        $scope.username + '/' + $scope.period
      
      rulesKey = ->
        $scope.username + '/rules'
        
      $scope.selectPeriod = (period, options) ->
        $scope.period = period
        $scope.transactions = []
        if $window.localStorage[periodKey()]? and options?.refresh != true
          console.log('Loading transactions from localStorage')
          $scope.transactions = angular.fromJson($window.localStorage[periodKey()])
          updateDays()
        else
          console.log('Loading transactions from website')
          $scope.tasks[period] =
            name: 'Transacties ophalen voor "' + period + '"'
            progress: 100
          socket.send(angular.toJson(period))

      $scope.periodTotalSelected = ->
        _(_($scope.days).values()).filter((day) -> day.selected).reduce(((memo, day) -> memo + day.total), 0)

      $scope.periodTotal = ->
        _(_($scope.days).values()).reduce(((memo, day) -> memo + day.total), 0)

      $scope.addDestination = (destination) ->
        if $scope.rules.destinations[destination]?
          $scope.rules.destinations[destination] = !$scope.rules.destinations[destination]
        else
          $scope.rules.destinations[destination] = true

      $scope.removeDestination = (destination) ->
        delete $scope.rules.destinations[destination]
          
      $scope.createPdf = ->
        selectedTransactions = _(_(_(_($scope.days).filter((day) -> day.selected)).map((day) -> day.transactions)).flatten()).map((tx) -> tx.name)
        console.log('Create pdf', selectedTransactions)
        $scope.tasks[$scope.period + '/pdf'] =
          name: 'PDF maken voor "' + $scope.period + '" (' + selectedTransactions.length + ' transacties)'
          progress: 100
        socket.send(angular.toJson(
          period: $scope.period
          transactions: selectedTransactions
        ))

      $scope.$watch('rules', (->
        if $scope.rules?
          $window.localStorage[rulesKey()] = angular.toJson($scope.rules)
          updateDays()
      ), true)

      socket = new WebSocket('ws://' + $window.location.host + '/ov')
      socket.onopen = ->
        focus('focusCommand')
      socket.onmessage = (msg) ->
        data = angular.fromJson(msg.data)
        console.log(data)
        $scope.$apply ->
          if data.error?
            $scope.loggedIn = false
            $scope.error = data.error
          if data.periods?
            $scope.periods = _(data.periods).filter (period) ->
              period == 'Deze maand' or period.match(/^[A-Z][a-z]+ \d\d\d\d$/)
            $window.localStorage.username = $scope.username
            $window.localStorage.password = $scope.password
            $scope.loggedIn = true
            $scope.loggingIn = false
            delete $scope.globalTasks['login']
            $scope.rules = angular.fromJson($window.localStorage[rulesKey()]) ||
              days: [false, true, true, true, true, true, false] # sun, mon, ..., sat
              destinations: {}
          if data.transaction? and data.transaction.period == $scope.period
            $scope.transactions.push(data.transaction)
            updateDays()
          if data.finished?
            delete $scope.tasks[data.finished]
            if data.finished == $scope.period
              console.log('Saving transactions to localStorage')
              $window.localStorage[periodKey()] = angular.toJson($scope.transactions)
          if data.pdf?
            delete $scope.tasks[data.pdf.period + '/pdf']
            if not $scope.pdfs[data.pdf.period]?
              $scope.pdfs[data.pdf.period] = []
            $scope.pdfs[data.pdf.period].push(
              timestamp: new Date()
              uuid: data.pdf.uuid
            )
  ])
  
  angular.bootstrap(document, ['ov-verwerker'])
)