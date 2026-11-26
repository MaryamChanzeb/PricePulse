# Bug log

Real faults found while building. The fix commit is filled in after the commit is made.

| # | What happened | Cause | How it was found | Fix | Fix commit |
|---|---------------|-------|------------------|-----|------------|
| B1 | Demo price history was 5 hours "in the future" compared with the server's clock | `seed_demo_history.sql` used `NOW(6)`, which is Pakistan time, while the server stores all times in UTC (`hibernate.jdbc.time_zone=UTC`) | Running the script and comparing `MAX(observed_at)` with `UTC_TIMESTAMP()` in MySQL Workbench | Use `UTC_TIMESTAMP(6)` everywhere in the demo seed | Fixed before the file was first committed (Z1, "Add labelled demo price history ...") |
| B2 | A price 60.001% above the last accepted price was ACCEPTED instead of FLAGGED (FR-13) | The change percent was divided and rounded to 2 decimals (60.001 -> 60.00) before comparing with the 60% limit | JUnit test `ObservationControllerTest.changeOfExactlySixtyPercentIsStillAccepted` failed: expected FLAGGED but was ACCEPTED for Rs 1,000 -> Rs 1,600.01 | Compare exactly without dividing: \|new - last\| x 100 > last x 60 | Z4 commit "Add first version of the observation endpoint ..." |
| B3 | Three rule tests failed: the rules saw one observation fewer than the prepared history had ("3 observation(s) over 4 day(s)" instead of 4 over 5) | Test helper bug: in `evaluate(h.newestFirst(), h.now(...))` Java evaluates the arguments left to right, so the history list was copied before the current observation was added to it | `mvn test`: DiscountAnalysisServiceTest and PerpetualSaleRuleTest failed with the message above | `History.newestFirst()` returns a live, always-sorted view instead of a copy | Z5 commit "Add JUnit tests with prepared price histories ..." |
