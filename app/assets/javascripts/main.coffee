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

  module.controller('MainCtrl', [ '$scope', '$window', 'focus',
    ($scope, $window, focus) ->
      $scope.loggedIn = false
      $scope.username = $window.localStorage.username || ''
      $scope.password = $window.localStorage.password || ''
      $scope.periods = []
      $scope.transactions = []
      $scope.days = {}
      $scope.destinations = angular.fromJson($window.localStorage.destinations) || []
      
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
            date: date
            transactions: txs
            destination: destination?.destination
            total: _(txs).reduce(((memo, tx) -> memo + tx.price), 0)
          console.log(result)
          result
        console.log($scope.days)
      
      $scope.login = ->
        socket.send(
          angular.toJson(
            username: $scope.username
            password: $scope.password
          )
        )
        
      $scope.selectPeriod = (period) ->
        $scope.period = period
        $scope.transactions = []
        socket.send(angular.toJson(period))

      $scope.addDestination = (destination) ->
        $scope.destinations = _($scope.destinations).union([destination])
        $window.localStorage.destinations = angular.toJson($scope.destinations)
        updateDays()
    
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
          if data.transaction?
            $scope.transactions.push(data.transaction)
            updateDays()
  ])
  
  angular.bootstrap(document, ['ov-verwerker'])
)