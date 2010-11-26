$(function() {
  $(".project").mouseenter(function() {
    $(this).children(".details").show();
  });

  $(".project").mouseleave(function() {
    $(this).children(".details").hide();
  });
});