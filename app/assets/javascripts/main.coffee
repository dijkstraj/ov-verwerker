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
            $scope.periods = data.periods
            $window.localStorage.username = $scope.username
            $window.localStorage.password = $scope.password
            $scope.loggedIn = true
          if data.transaction?
            $scope.transactions.push(data.transaction)
  ])
  
  angular.bootstrap(document, ['ov-verwerker'])
)