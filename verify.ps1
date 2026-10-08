Set-Location C:\RecoMind
Write-Host "=== GIT ===" ; git log --oneline -5 ; git status --short
Write-Host "=== BUILD ===" ; .\gradlew.bat clean test
$t=0; $f=0
Get-ChildItem build\test-results\test\*.xml -ErrorAction SilentlyContinue | ForEach-Object {
  $x=[xml](Get-Content $_.FullName)
  $t+=[int]$x.testsuite.tests; $f+=[int]$x.testsuite.failures+[int]$x.testsuite.errors }
Write-Host "=== TESTS: $t run, $f failed ==="