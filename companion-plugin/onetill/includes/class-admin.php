<?php
/**
 * WP Admin interface.
 *
 * Registers the OneTill admin menu, renders the dashboard and settings
 * pages, manages device connections, and adds barcode fields to the
 * product edit screen.
 *
 * @package OneTill
 */

namespace OneTill;

defined( 'ABSPATH' ) || exit;

/**
 * Class Admin
 */
class Admin {

	/**
	 * Register the admin menu.
	 */
	public function register_menu() {
		add_menu_page(
			__( 'OneTill', 'onetill' ),
			__( 'OneTill', 'onetill' ),
			'manage_woocommerce',
			'onetill',
			array( $this, 'render_dashboard_page' ),
			'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDI0IDEwMjQiPjxkZWZzPjxtYXNrIGlkPSJtIj48cmVjdCB3aWR0aD0iMTAyNCIgaGVpZ2h0PSIxMDI0IiBmaWxsPSJ3aGl0ZSIvPjxwYXRoIGZpbGw9ImJsYWNrIiBkPSJNODA3LjUxNSAzNzAuOTg4QzgwOC45ODggMzcyLjU1OCA4MDguNDA0IDQwMS42OTUgODA4LjQwNCA0MDUuNzI0TDgwOC40MTIgNDcxLjQ3M0w4MDguNDI2IDY3MC44OTRDODI4LjQzMyA2NjIuMDA2IDg0OS41NTkgNjUyLjI3IDg2OS44MzggNjQ0LjA0MUM4NzAuMTMxIDY2Ny4yOTkgODcwLjE5NiA2OTAuNTU5IDg3MC4wMzMgNzEzLjgxOEM4NDAuNjA0IDcyNi4xOTcgODExLjM2MSA3MzkuMDE3IDc4Mi4zMTUgNzUyLjI3MUw2NzIuOTg2IDgwMC41ODJMNjcyLjkzNSA3NzEuMjVDNjcyLjk3MSA3NTcuNjA2IDY3Mi44MjMgNzQzLjk2MiA2NzIuNDkxIDczMC4zMjNDNjkyLjg5NiA3MjEuMDc0IDcxNC42MzggNzEyLjc2NiA3MzQuNTcxIDcwMi45NzZMNzM0LjQ3NSA1NjMuNzY3TDczNC40MjIgNTIwLjcwM0M3MzQuNDE0IDUxMi4yMiA3MzQuMTQ0IDUwMS45NzIgNzM0Ljg3NSA0OTMuNjcxQzcxNi4xOSA1MDguMzU4IDY5Ny43NTkgNTI1LjA0MiA2NzkuMzE5IDU0MC4zMzNDNjczLjMwMyA1MjAuMTU0IDY2OC40NDggNDk4LjkzMiA2NjIuNjE1IDQ3OC40NTZDNjgzLjQzNyA0NjEuNjI2IDcwMi45MzEgNDQyLjk0IDcyMi4zNjMgNDI0LjQ3OUM3MzIuMzAyIDQxNS4wMzcgNzQzLjgzNiA0MDIuMzc0IDc1NC41NTIgMzk0LjEzM0M3NTcuMzAxIDM5Mi4wMiA3NzEuMTQ0IDM4Ni4zNjMgNzc1LjA0MSAzODQuNzExTDgwNy41MTUgMzcwLjk4OFoiLz48L21hc2s+PC9kZWZzPjxnIG1hc2s9InVybCgjbSkiPjxwYXRoIGQ9Ik02MjEuMDMgMjE3LjMyOEw4MTQuNzggMTM0LjU3TDg4OS45NDIgMTAyLjUxNEM5MDEuMTY2IDk3Ljc3NzcgOTEzLjIwOCA5Mi4zNzg3IDkyNC41MTMgODguMDUzOUM5MzQuMTE1IDkxLjk0OTEgOTQ3LjEgOTguOTMxOSA5NTYuNSAxMDMuNDkzQzk3OS4xMDQgMTE0LjQ1OSAxMDAxLjQyIDEyNi4zMjcgMTAyNCAxMzcuMzU3TDEwMjQgMTQxLjEyNEwxMDI0IDc3OC43MTNMNjQxLjk3OSA5NTAuMDU3TDUzMS44NTIgOTk5LjUzNEM1MTkuOTM4IDEwMDQuNzggNTA4LjA2NCAxMDEwLjExIDQ5Ni4yMjkgMTAxNS41MkM0OTEuNTY4IDEwMTcuNjUgNDgyLjM4NiAxMDIxLjUzIDQ3OC4zNDkgMTAyNEw0NzYuMzk4IDEwMjRMNDc2LjExOSAxMDI0QzQ3My4xNDUgMTAyMS43NSA0NjYuMzggMTAxOC4yNyA0NjIuNzIxIDEwMTYuMjRDNDU1LjIwMyAxMDEyLjA2IDQ0Ny43MjcgMTAwNy44MiA0NDAuMjkyIDEwMDMuNUM0MDcuNTIxIDk4NC41MDMgMzc0LjYyMyA5NjUuNzI1IDM0MS41OTkgOTQ3LjE2OUwwIDc1NS42OTdMMCA1OTYuMTI4TDAgNTk0LjAwNEMyLjkxOTggNTkyLjM1OSAxMS4wNDg5IDU4OC45OTUgMTQuNDc3NiA1ODcuNDY3TDQ0LjA5MDIgNTc0LjIwN0wxMzguMjU1IDUzMS43NjJDMTM3Ljc5NSA1MjcuODA4IDEzNy43NTkgNTE5LjE1NyAxMzguNDEzIDUxNS4yMTVDMTM4LjY2MyA1MTMuNzA2IDEzOS41MzIgNTEzLjQ1MyAxNDAuNTk1IDUxMi44ODNDMTQ3LjE1OCA1MDkuNTM3IDE1NS43ODcgNTA1Ljk4NiAxNjIuNjgyIDUwMi45NUwyMDYuNjk3IDQ4My43MjRMMzQ4LjA2NiA0MjIuMTJDMzUxLjI5OSA0MTguODE1IDM2MS41NTEgMzI5LjU3OSAzNjUuMjM2IDMyNy4wNTFDMzY5LjI2NiAzMjUuMTc4IDM3My42MDIgMzIzLjM1NyAzNzcuNzI0IDMyMS42NkM0MjcuMzQ4IDMwMS4yMzcgNDc2LjM2NCAyNzguMzM0IDUyNi4wMjcgMjU4LjEwNkM1MjYuODA0IDI2MC4zMjggNTI2LjYwMSAyNzkuODc0IDUyNi44OTYgMjg0LjMyMkM1MzIuNjg0IDI4Ny41MjQgNTYxLjEwMSAzMDMuNjY0IDU2NC45NDEgMzA0LjM2MkM1ODMuNjI1IDI5Ni4wNTEgNjAyLjQ2IDI4OC4wODMgNjIxLjQzNSAyODAuNDYyTDYyMS4wMyAyMTcuMzI4WiIvPjxwYXRoIGQ9Ik0xMzguMjU1IDUzMS43NjJDMTM5LjE1NCA1MzkuODY0IDEzOC41NDUgNTU5LjcwMyAxMzguNTYyIDU2OC42OTFMMTM5LjAwMiA2MzIuNDRMMzg4LjI3NCA3NjcuODE2TDQ0OS41NzQgODAxLjUxNUM0NTMuMDM1IDgwMy40MjIgNDcxLjYzOSA4MTMuNDQgNDczLjM4MiA4MTQuOTc5QzQ3My42OCA4MTYuNTg4IDQ3My43OTEgODE3LjQwOSA0NzMuOTI3IDgxOS4wNEM0NzMuOTQyIDgyOC4xNzYgNDczLjY5OSA4MzkuMjkxIDQ3NC4wNzEgODQ4LjIzM0M0NzQuOTMgODUzLjUxMSA0NzQuNjE3IDg2OS44MTEgNDc0LjY0MyA4NzYuMjAyTDQ3NS4wMTkgOTI3LjAwNUM0NzUuMTYzIDk1NS4xMDUgNDc2LjEzNiA5ODMuMTk5IDQ3Ni4zMzggMTAxMS4zQzQ3Ni4zNjggMTAxNS41MyA0NzYuNTI3IDEwMTkuNzcgNDc2LjM5OCAxMDI0TDQ3Ni4xMTkgMTAyNEM0NzMuMTQ1IDEwMjEuNzUgNDY2LjM4IDEwMTguMjcgNDYyLjcyMSAxMDE2LjI0QzQ1NS4yMDMgMTAxMi4wNiA0NDcuNzI3IDEwMDcuODIgNDQwLjI5MiAxMDAzLjVDNDA3LjUyMSA5ODQuNTAzIDM3NC42MjMgOTY1LjcyNSAzNDEuNTk5IDk0Ny4xNjlMMCA3NTUuNjk3TDAgNTk2LjEyOEwwIDU5NC4wMDRDMi45MTk4IDU5Mi4zNTkgMTEuMDQ4OSA1ODguOTk1IDE0LjQ3NzYgNTg3LjQ2N0w0NC4wOTAyIDU3NC4yMDdMMTM4LjI1NSA1MzEuNzYyWiIvPjxwYXRoIGQ9Ik0xMzguMjU1IDUzMS43NjJDMTM5LjE1NCA1MzkuODY0IDEzOC41NDUgNTU5LjcwMyAxMzguNTYyIDU2OC42OTFMMTM5LjAwMiA2MzIuNDRMMzg4LjI3NCA3NjcuODE2TDQ0OS41NzQgODAxLjUxNUM0NTMuMDM1IDgwMy40MjIgNDcxLjYzOSA4MTMuNDQgNDczLjM4MiA4MTQuOTc5QzQ3My42OCA4MTYuNTg4IDQ3My43OTEgODE3LjQwOSA0NzMuOTI3IDgxOS4wNEM0NzMuOTQyIDgyOC4xNzYgNDczLjY5OSA4MzkuMjkxIDQ3NC4wNzEgODQ4LjIzM0M0NzMuOTMyIDg1MC45NDUgNDc0LjA5NCA4NTQuODQ3IDQ3NC4xMyA4NTcuNjUxTDE0OC4yODggNjc3LjgzOUw0NC41Mjg1IDYyMC43OTRDMjkuNzQ3MSA2MTIuNjUzIDE0LjgyODYgNjA0LjA2IDAgNTk2LjEyOEwwIDU5NC4wMDRDMi45MTk4IDU5Mi4zNTkgMTEuMDQ4OSA1ODguOTk1IDE0LjQ3NzYgNTg3LjQ2N0w0NC4wOTAyIDU3NC4yMDdMMTM4LjI1NSA1MzEuNzYyWiIvPjxwYXRoIGQ9Ik0zNDguMDY2IDQyMi4xMkMzNTEuMjk5IDQxOC44MTUgMzYxLjU1MSAzMjkuNTc5IDM2NS4yMzYgMzI3LjA1MUMzODUuODY4IDMzNi40NjcgNDA3LjA1NCAzNDcuMDg4IDQyNy41MDcgMzU3LjAxNkM0MzkuOTIxIDM2My4wNDEgNDU4LjkxOSAzNzEuNTQgNDcwLjA1OSAzNzguNDk5QzQ2OC44NjUgMzgyLjIxMiA0NjkuNzg5IDQxNC45MzkgNDY5Ljg4IDQyMS4yOTRMNDcwLjUyNiA0NzguODI0TDQ3My4wMjEgNzIwLjExMUw0NzMuOTMgNzg2LjM1M0M0NzQuMDQxIDc5Ni41MyA0NzQuNDA1IDgwOC45NzcgNDczLjkyNyA4MTkuMDRDNDczLjc5MSA4MTcuNDA5IDQ3My42OCA4MTYuNTg4IDQ3My4zODIgODE0Ljk3OUM0NzEuNjM5IDgxMy40NCA0NTMuMDM1IDgwMy40MjIgNDQ5LjU3NCA4MDEuNTE1TDM4OC4yNzQgNzY3LjgxNkwxMzkuMDAyIDYzMi40NEwxMzguNTYyIDU2OC42OTFDMTM4LjU0NSA1NTkuNzAzIDEzOS4xNTQgNTM5Ljg2NCAxMzguMjU1IDUzMS43NjJDMTM3Ljc5NSA1MjcuODA4IDEzNy43NTkgNTE5LjE1NyAxMzguNDEzIDUxNS4yMTVDMTM4LjY2MyA1MTMuNzA2IDEzOS41MzIgNTEzLjQ1MyAxNDAuNTk1IDUxMi44ODNDMTQ3LjE1OCA1MDkuNTM3IDE1NS43ODcgNTA1Ljk4NiAxNjIuNjgyIDUwMi45NUwyMDYuNjk3IDQ4My43MjRMMzQ4LjA2NiA0MjIuMTJaIi8+PHBhdGggZD0iTTM0OC4wNjYgNDIyLjEyQzM0OC45NTkgNDI0LjI1MSAzNDQuNjEyIDQ0Ni4xNjYgMzQzLjk3OCA0NTAuMDY0TDMzNC42MTQgNTA3Ljk1OEMyNzAuNzg2IDUwOS4wMSAyMDYuODE2IDUxMi4xMTUgMTQyLjk4MSA1MTMuNDUxQzE0Mi4wNTEgNTEzLjQ3IDE0MS40MjEgNTEzLjI5NCAxNDAuNTk1IDUxMi44ODNDMTQ3LjE1OCA1MDkuNTM3IDE1NS43ODcgNTA1Ljk4NiAxNjIuNjgyIDUwMi45NUwyMDYuNjk3IDQ4My43MjRMMzQ4LjA2NiA0MjIuMTJaIi8+PHBhdGggZD0iTTYyMS4wMyAyMTcuMzI4TDgxNC43OCAxMzQuNTdMODg5Ljk0MiAxMDIuNTE0QzkwMS4xNjYgOTcuNzc3NyA5MTMuMjA4IDkyLjM3ODcgOTI0LjUxMyA4OC4wNTM5QzkzNC4xMTUgOTEuOTQ5MSA5NDcuMSA5OC45MzE5IDk1Ni41IDEwMy40OTNDOTc5LjEwNCAxMTQuNDU5IDEwMDEuNDIgMTI2LjMyNyAxMDI0IDEzNy4zNTdMMTAyNCAxNDEuMTI0TDY2Ni45MjkgMjk0LjI3NEw1MzMuODYgMzUxLjA3Mkw0OTMuMTM1IDM2OC41MTZDNDg1LjU4NSAzNzEuNzQ0IDQ3Ny4zNTggMzc0Ljk0NCA0NzAuMDU5IDM3OC40OTlDNDU4LjkxOSAzNzEuNTQgNDM5LjkyMSAzNjMuMDQxIDQyNy41MDcgMzU3LjAxNkM0MDcuMDU0IDM0Ny4wODggMzg1Ljg2OCAzMzYuNDY3IDM2NS4yMzYgMzI3LjA1MUMzNjkuMjY2IDMyNS4xNzggMzczLjYwMiAzMjMuMzU3IDM3Ny43MjQgMzIxLjY2QzQyNy4zNDggMzAxLjIzNyA0NzYuMzY0IDI3OC4zMzQgNTI2LjAyNyAyNTguMTA2QzUyNi44MDQgMjYwLjMyOCA1MjYuNjAxIDI3OS44NzQgNTI2Ljg5NiAyODQuMzIyQzUzMi42ODQgMjg3LjUyNCA1NjEuMTAxIDMwMy42NjQgNTY0Ljk0MSAzMDQuMzYyQzU4My42MjUgMjk2LjA1MSA2MDIuNDYgMjg4LjA4MyA2MjEuNDM1IDI4MC40NjJMNjIxLjAzIDIxNy4zMjhaIi8+PHBhdGggZD0iTTY0MC42NDYgMEw2NDIuOTA4IDBDNjU5Ljc0MiAxMC40NTQ4IDY3OS4wNiAxOS4yNDczIDY5NS42MyAyOS44NzE4QzY5NC44MzUgNDAuNTEyMiA2OTUuNzExIDU2LjA5NzEgNjk1LjUwOSA2Ny4xOTIyQzY5NC45NTUgOTcuNjI2MiA2OTYuMzQxIDEyOS42MDEgNjk1LjQ3NiAxNTkuOTEzQzY3MC45NjUgMTcxLjMyMiA2NDUuMTQyIDE4MC44NiA2MjAuNTcyIDE5Mi4wNjFDNjIxLjMyIDE5OC44NDMgNjIxLjA0MiAyMTAuMjc0IDYyMS4wMyAyMTcuMzI4TDYyMS40MzUgMjgwLjQ2MkM2MDIuNDYgMjg4LjA4MyA1ODMuNjI1IDI5Ni4wNTEgNTY0Ljk0MSAzMDQuMzYyQzU2MS4xMDEgMzAzLjY2NCA1MzIuNjg0IDI4Ny41MjQgNTI2Ljg5NiAyODQuMzIyQzUyNi42MDEgMjc5Ljg3NCA1MjYuODA0IDI2MC4zMjggNTI2LjAyNyAyNTguMTA2QzUyNy4xODIgMjU2LjEwNSA1MjYuNDMxIDIzNS4wNTQgNTI2LjE0IDIzMS40MjRDNTA1LjE2NCAyMzkuODg4IDQ4My43NDMgMjUwLjYxOSA0NjMuMTkzIDI1OC43MjhDNDU5Ljk1MSAyNTguMzU2IDQxNS43MzYgMjMyLjYyNSA0MDkuNTEyIDIyOS4xMDFDNDA4LjA3MiAxOTUuNDY5IDQwOS42ODUgMTYxLjEzIDQwOC44MzkgMTI3LjQyQzQwOC42MTcgMTE4LjU4IDQwOC40MjUgMTA4LjQ4MyA0MDkuMDIzIDk5LjcxNzVDNDE1LjM0MiA5Ni4xNjM2IDQyNi4zIDkxLjcyNTIgNDMzLjIzMSA4OC43MjcxTDQ3NC44ODEgNzAuODhMNTkxLjI5MiAyMS40NDQ5TDYyMC4yMjggOS4yNzQ2M0M2MjYuNjc2IDYuNTc1MzkgNjM0LjY0NSAzLjQwNzcgNjQwLjY0NiAwWiIvPjxwYXRoIGQ9Ik02NDAuNjQ2IDBMNjQyLjkwOCAwQzY1OS43NDIgMTAuNDU0OCA2NzkuMDYgMTkuMjQ3MyA2OTUuNjMgMjkuODcxOEM2OTIuNDQ3IDMxLjcxNjMgNjg2LjQxOCAzNC4wMTkzIDY4Mi44NiAzNS41MDc1QzY3NS44MyAzOC40MjQ5IDY2OC44MjcgNDEuNDEwOSA2NjEuODU1IDQ0LjQ2NTFMNTcxLjI4OSA4My4yN0w0ODYuODc3IDExOS40NjlDNDgwLjkyNSAxMjEuOTYyIDQ2Ni4xNzEgMTI5LjI1MyA0NjIuMDE1IDEzMC41OTJDNDU1LjEwNiAxMjUuNzEzIDQ0Ni4yMzEgMTIwLjk2IDQzOC43OTUgMTE2LjY3QzQyOS4xODEgMTExLjEyNCA0MTguODg4IDEwNC42MjUgNDA5LjAyMyA5OS43MTc1QzQxNS4zNDIgOTYuMTYzNiA0MjYuMyA5MS43MjUyIDQzMy4yMzEgODguNzI3MUw0NzQuODgxIDcwLjg4TDU5MS4yOTIgMjEuNDQ0OUw2MjAuMjI4IDkuMjc0NjNDNjI2LjY3NiA2LjU3NTM5IDYzNC42NDUgMy40MDc3IDY0MC42NDYgMFoiLz48cGF0aCBkPSJNNDA5LjAyMyA5OS43MTc1QzQxOC44ODggMTA0LjYyNSA0MjkuMTgxIDExMS4xMjQgNDM4Ljc5NSAxMTYuNjdDNDQ2LjIzMSAxMjAuOTYgNDU1LjEwNiAxMjUuNzEzIDQ2Mi4wMTUgMTMwLjU5MkM0NjIuMDYyIDE2MC43ODUgNDYyLjM4MSAxOTAuOTc3IDQ2Mi45NzMgMjIxLjE2NEM0NjMuMTE2IDIzMS4xMjggNDYyLjQyNSAyNDkuNjQ4IDQ2My4xOTMgMjU4LjcyOEM0NTkuOTUxIDI1OC4zNTYgNDE1LjczNiAyMzIuNjI1IDQwOS41MTIgMjI5LjEwMUM0MDguMDcyIDE5NS40NjkgNDA5LjY4NSAxNjEuMTMgNDA4LjgzOSAxMjcuNDJDNDA4LjYxNyAxMTguNTggNDA4LjQyNSAxMDguNDgzIDQwOS4wMjMgOTkuNzE3NVoiLz48cGF0aCBkPSJNNTI2LjE0IDIzMS40MjRDNTM4Ljc5OCAyMjYuMjk2IDU1MS42MTMgMjIwLjU0MiA1NjQuMzA2IDIxNS4yNThDNTY0LjcwMyAyNDQuODQgNTY0LjAyNiAyNzQuODk4IDU2NC45NDEgMzA0LjM2MkM1NjEuMTAxIDMwMy42NjQgNTMyLjY4NCAyODcuNTI0IDUyNi44OTYgMjg0LjMyMkM1MjYuNjAxIDI3OS44NzQgNTI2LjgwNCAyNjAuMzI4IDUyNi4wMjcgMjU4LjEwNkM1MjcuMTgyIDI1Ni4xMDUgNTI2LjQzMSAyMzUuMDU0IDUyNi4xNCAyMzEuNDI0WiIvPjwvZz48L3N2Zz4=',TM5LjAwMiA2MzIuNDRMMTM4LjU2MiA1NjguNjkxQzEzOC41NDUgNTU5LjcwMyAxMzkuMTU0IDUzOS44NjQgMTM4LjI1NSA1MzEuNzYyQzEzNy43OTUgNTI3LjgwOCAxMzcuNzU5IDUxOS4xNTcgMTM4LjQxMyA1MTUuMjE1QzEzOC42NjMgNTEzLjcwNiAxMzkuNTMyIDUxMy40NTMgMTQwLjU5NSA1MTIuODgzQzE0Ny4xNTggNTA5LjUzNyAxNTUuNzg3IDUwNS45ODYgMTYyLjY4MiA1MDIuOTVMMjA2LjY5NyA0ODMuNzI0TDM0OC4wNjYgNDIyLjEyWiIvPjxwYXRoICBkPSJNMzQ4LjA2NiA0MjIuMTJDMzQ4Ljk1OSA0MjQuMjUxIDM0NC42MTIgNDQ2LjE2NiAzNDMuOTc4IDQ1MC4wNjRMMzM0LjYxNCA1MDcuOTU4QzI3MC43ODYgNTA5LjAxIDIwNi44MTYgNTEyLjExNSAxNDIuOTgxIDUxMy40NTFDMTQyLjA1MSA1MTMuNDcgMTQxLjQyMSA1MTMuMjk0IDE0MC41OTUgNTEyLjg4M0MxNDcuMTU4IDUwOS41MzcgMTU1Ljc4NyA1MDUuOTg2IDE2Mi42ODIgNTAyLjk1TDIwNi42OTcgNDgzLjcyNEwzNDguMDY2IDQyMi4xMloiLz48cGF0aCAgZD0iTTYyMS4wMyAyMTcuMzI4TDgxNC43OCAxMzQuNTdMODg5Ljk0MiAxMDIuNTE0QzkwMS4xNjYgOTcuNzc3NyA5MTMuMjA4IDkyLjM3ODcgOTI0LjUxMyA4OC4wNTM5QzkzNC4xMTUgOTEuOTQ5MSA5NDcuMSA5OC45MzE5IDk1Ni41IDEwMy40OTNDOTc5LjEwNCAxMTQuNDU5IDEwMDEuNDIgMTI2LjMyNyAxMDI0IDEzNy4zNTdMMTAyNCAxNDEuMTI0TDY2Ni45MjkgMjk0LjI3NEw1MzMuODYgMzUxLjA3Mkw0OTMuMTM1IDM2OC41MTZDNDg1LjU4NSAzNzEuNzQ0IDQ3Ny4zNTggMzc0Ljk0NCA0NzAuMDU5IDM3OC40OTlDNDU4LjkxOSAzNzEuNTQgNDM5LjkyMSAzNjMuMDQxIDQyNy41MDcgMzU3LjAxNkM0MDcuMDU0IDM0Ny4wODggMzg1Ljg2OCAzMzYuNDY3IDM2NS4yMzYgMzI3LjA1MUMzNjkuMjY2IDMyNS4xNzggMzczLjYwMiAzMjMuMzU3IDM3Ny43MjQgMzIxLjY2QzQyNy4zNDggMzAxLjIzNyA0NzYuMzY0IDI3OC4zMzQgNTI2LjAyNyAyNTguMTA2QzUyNi44MDQgMjYwLjMyOCA1MjYuNjAxIDI3OS44NzQgNTI2Ljg5NiAyODQuMzIyQzUzMi42ODQgMjg3LjUyNCA1NjEuMTAxIDMwMy42NjQgNTY0Ljk0MSAzMDQuMzYyQzU4My42MjUgMjk2LjA1MSA2MDIuNDYgMjg4LjA4MyA2MjEuNDM1IDI4MC40NjJMNjIxLjAzIDIxNy4zMjhaIi8+PHBhdGggIGQ9Ik02NDAuNjQ2IDBMNjQyLjkwOCAwQzY1OS43NDIgMTAuNDU0OCA2NzkuMDYgMTkuMjQ3MyA2OTUuNjMgMjkuODcxOEM2OTQuODM1IDQwLjUxMjIgNjk1LjcxMSA1Ni4wOTcxIDY5NS41MDkgNjcuMTkyMkM2OTQuOTU1IDk3LjYyNjIgNjk2LjM0MSAxMjkuNjAxIDY5NS40NzYgMTU5LjkxM0M2NzAuOTY1IDE3MS4zMjIgNjQ1LjE0MiAxODAuODYgNjIwLjU3MiAxOTIuMDYxQzYyMS4zMiAxOTguODQzIDYyMS4wNDIgMjEwLjI3NCA2MjEuMDMgMjE3LjMyOEw2MjEuNDM1IDI4MC40NjJDNjAyLjQ2IDI4OC4wODMgNTgzLjYyNSAyOTYuMDUxIDU2NC45NDEgMzA0LjM2MkM1NjEuMTAxIDMwMy42NjQgNTMyLjY4NCAyODcuNTI0IDUyNi44OTYgMjg0LjMyMkM1MjYuNjAxIDI3OS44NzQgNTI2LjgwNCAyNjAuMzI4IDUyNi4wMjcgMjU4LjEwNkM1MjcuMTgyIDI1Ni4xMDUgNTI2LjQzMSAyMzUuMDU0IDUyNi4xNCAyMzEuNDI0QzUwNS4xNjQgMjM5Ljg4OCA0ODMuNzQzIDI1MC42MTkgNDYzLjE5MyAyNTguNzI4QzQ1OS45NTEgMjU4LjM1NiA0MTUuNzM2IDIzMi42MjUgNDA5LjUxMiAyMjkuMTAxQzQwOC4wNzIgMTk1LjQ2OSA0MDkuNjg1IDE2MS4xMyA0MDguODM5IDEyNy40MkM0MDguNjE3IDExOC41OCA0MDguNDI1IDEwOC40ODMgNDA5LjAyMyA5OS43MTc1QzQxNS4zNDIgOTYuMTYzNiA0MjYuMyA5MS43MjUyIDQzMy4yMzEgODguNzI3MUw0NzQuODgxIDcwLjg4TDU5MS4yOTIgMjEuNDQ0OUw2MjAuMjI4IDkuMjc0NjNDNjI2LjY3NiA2LjU3NTM5IDYzNC42NDUgMy40MDc3IDY0MC42NDYgMFoiLz48cGF0aCAgZD0iTTY0MC42NDYgMEw2NDIuOTA4IDBDNjU5Ljc0MiAxMC40NTQ4IDY3OS4wNiAxOS4yNDczIDY5NS42MyAyOS44NzE4QzY5Mi40NDcgMzEuNzE2MyA2ODYuNDE4IDM0LjAxOTMgNjgyLjg2IDM1LjUwNzVDNjc1LjgzIDM4LjQyNDkgNjY4LjgyNyA0MS40MTA5IDY2MS44NTUgNDQuNDY1MUw1NzEuMjg5IDgzLjI3TDQ4Ni44NzcgMTE5LjQ2OUM0ODAuOTI1IDEyMS45NjIgNDY2LjE3MSAxMjkuMjUzIDQ2Mi4wMTUgMTMwLjU5MkM0NTUuMTA2IDEyNS43MTMgNDQ2LjIzMSAxMjAuOTYgNDM4Ljc5NSAxMTYuNjdDNDI5LjE4MSAxMTEuMTI0IDQxOC44ODggMTA0LjYyNSA0MDkuMDIzIDk5LjcxNzVDNDE1LjM0MiA5Ni4xNjM2IDQyNi4zIDkxLjcyNTIgNDMzLjIzMSA4OC43MjcxTDQ3NC44ODEgNzAuODhMNTkxLjI5MiAyMS40NDQ5TDYyMC4yMjggOS4yNzQ2M0M2MjYuNjc2IDYuNTc1MzkgNjM0LjY0NSAzLjQwNzcgNjQwLjY0NiAwWiIvPjxwYXRoICBkPSJNNDA5LjAyMyA5OS43MTc1QzQxOC44ODggMTA0LjYyNSA0MjkuMTgxIDExMS4xMjQgNDM4Ljc5NSAxMTYuNjdDNDQ2LjIzMSAxMjAuOTYgNDU1LjEwNiAxMjUuNzEzIDQ2Mi4wMTUgMTMwLjU5MkM0NjIuMDYyIDE2MC43ODUgNDYyLjM4MSAxOTAuOTc3IDQ2Mi45NzMgMjIxLjE2NEM0NjMuMTE2IDIzMS4xMjggNDYyLjQyNSAyNDkuNjQ4IDQ2My4xOTMgMjU4LjcyOEM0NTkuOTUxIDI1OC4zNTYgNDE1LjczNiAyMzIuNjI1IDQwOS41MTIgMjI5LjEwMUM0MDguMDcyIDE5NS40NjkgNDA5LjY4NSAxNjEuMTMgNDA4LjgzOSAxMjcuNDJDNDA4LjYxNyAxMTguNTggNDA4LjQyNSAxMDguNDgzIDQwOS4wMjMgOTkuNzE3NVoiLz48cGF0aCAgZD0iTTUyNi4xNCAyMzEuNDI0QzUzOC43OTggMjI2LjI5NiA1NTEuNjEzIDIyMC41NDIgNTY0LjMwNiAyMTUuMjU4QzU2NC43MDMgMjQ0Ljg0IDU2NC4wMjYgMjc0Ljg5OCA1NjQuOTQxIDMwNC4zNjJDNTYxLjEwMSAzMDMuNjY0IDUzMi42ODQgMjg3LjUyNCA1MjYuODk2IDI4NC4zMjJDNTI2LjYwMSAyNzkuODc0IDUyNi44MDQgMjYwLjMyOCA1MjYuMDI3IDI1OC4xMDZDNTI3LjE4MiAyNTYuMTA1IDUyNi40MzEgMjM1LjA1NCA1MjYuMTQgMjMxLjQyNFoiLz48L3N2Zz4=',
			56
		);

		add_submenu_page(
			'onetill',
			__( 'Dashboard', 'onetill' ),
			__( 'Dashboard', 'onetill' ),
			'manage_woocommerce',
			'onetill',
			array( $this, 'render_dashboard_page' )
		);

		add_submenu_page(
			'onetill',
			__( 'Users', 'onetill' ),
			__( 'Users', 'onetill' ),
			'manage_woocommerce',
			'onetill-users',
			array( $this, 'render_users_page' )
		);

		add_submenu_page(
			'onetill',
			__( 'Settings', 'onetill' ),
			__( 'Settings', 'onetill' ),
			'manage_woocommerce',
			'onetill-settings',
			array( $this, 'render_settings_page' )
		);
	}

	/**
	 * Enqueue admin CSS and JS on OneTill pages.
	 *
	 * @param string $hook_suffix The current admin page hook suffix.
	 */
	public function enqueue_assets( $hook_suffix ) {
		if ( false === strpos( $hook_suffix, 'onetill' ) ) {
			return;
		}

		wp_enqueue_style(
			'onetill-admin',
			ONETILL_PLUGIN_URL . 'assets/css/admin.css',
			array(),
			ONETILL_VERSION
		);

		wp_enqueue_script(
			'onetill-admin',
			ONETILL_PLUGIN_URL . 'assets/js/admin.js',
			array( 'jquery' ),
			ONETILL_VERSION,
			true
		);

		wp_localize_script( 'onetill-admin', 'onetillAdmin', array(
			'ajaxUrl' => admin_url( 'admin-ajax.php' ),
			'nonce'   => wp_create_nonce( 'onetill_admin' ),
			'i18n'    => array(
				'pairingTitle'      => __( 'Pair New Device', 'onetill' ),
				'waitingForDevice'  => __( 'Waiting for device to scan QR code...', 'onetill' ),
				'pairingComplete'   => __( 'Device connected!', 'onetill' ),
				'pairingExpired'    => __( 'QR code expired. Click "Pair New Device" to try again.', 'onetill' ),
				'pairingError'      => __( 'Something went wrong. Please try again.', 'onetill' ),
				'disconnectConfirm' => __( 'Are you sure you want to disconnect this device? It will need to be re-paired.', 'onetill' ),
				'disconnected'      => __( 'Device disconnected.', 'onetill' ),
				'never'             => __( 'Never', 'onetill' ),
				'addUser'           => __( 'Add User', 'onetill' ),
				'editUser'          => __( 'Edit User', 'onetill' ),
				'save'              => __( 'Save', 'onetill' ),
				'saving'            => __( 'Saving...', 'onetill' ),
				'nameRequired'      => __( 'First and last name are required.', 'onetill' ),
				'pinRequired'       => __( 'PIN is required.', 'onetill' ),
				'pinFormat'         => __( 'PIN must be exactly 4 digits.', 'onetill' ),
				'pinLeaveBlank'     => __( '(leave blank to keep current)', 'onetill' ),
				/* translators: %s: staff member's name */
				'deleteConfirm'     => __( 'Are you sure you want to delete %s?', 'onetill' ),
				'userError'         => __( 'Something went wrong. Please try again.', 'onetill' ),
			),
		) );
	}

	/**
	 * Render the dashboard page.
	 */
	public function render_dashboard_page() {
		$devices     = $this->get_devices();
		$https_ok    = is_ssl();
		$change_logs = $this->get_change_log_stats();

		include ONETILL_PLUGIN_DIR . 'templates/admin-dashboard.php';
	}

	/**
	 * Render the settings page.
	 */
	public function render_settings_page() {
		include ONETILL_PLUGIN_DIR . 'templates/admin-settings.php';
	}

	/**
	 * Render the users page.
	 */
	public function render_users_page() {
		$users = $this->get_users();

		include ONETILL_PLUGIN_DIR . 'templates/admin-users.php';
	}

	/**
	 * AJAX handler: Create a user.
	 */
	public function ajax_create_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$first_name = isset( $_POST['first_name'] ) ? sanitize_text_field( wp_unslash( $_POST['first_name'] ) ) : '';
		$last_name  = isset( $_POST['last_name'] ) ? sanitize_text_field( wp_unslash( $_POST['last_name'] ) ) : '';
		$pin        = isset( $_POST['pin'] ) ? sanitize_text_field( wp_unslash( $_POST['pin'] ) ) : '';

		if ( empty( $first_name ) || empty( $last_name ) || empty( $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'All fields are required.', 'onetill' ) ), 400 );
		}

		if ( ! preg_match( '/^\d{4}$/', $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'PIN must be exactly 4 digits.', 'onetill' ) ), 400 );
		}

		if ( $this->is_pin_taken( $pin ) ) {
			wp_send_json_error( array( 'message' => __( 'This PIN is already in use. Please choose a different one.', 'onetill' ) ), 409 );
		}

		global $wpdb;

		$now = current_time( 'mysql', true );

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery -- custom table, no WP API available.
		$wpdb->insert(
			$wpdb->prefix . 'onetill_users',
			array(
				'first_name' => $first_name,
				'last_name'  => $last_name,
				'pin'        => wp_hash_password( $pin ),
				'pin_sha256' => hash( 'sha256', $pin ),
				'created_at' => $now,
				'updated_at' => $now,
			),
			array( '%s', '%s', '%s', '%s', '%s', '%s' )
		);

		$user_id = $wpdb->insert_id;

		if ( ! $user_id ) {
			wp_send_json_error( array( 'message' => __( 'Failed to create user.', 'onetill' ) ), 500 );
		}

		wp_send_json_success( array(
			'message' => __( 'User created.', 'onetill' ),
			'user'    => array(
				'id'         => $user_id,
				'first_name' => $first_name,
				'last_name'  => $last_name,
			),
		) );
	}

	/**
	 * AJAX handler: Update a user.
	 */
	public function ajax_update_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$user_id    = isset( $_POST['user_id'] ) ? absint( $_POST['user_id'] ) : 0;
		$first_name = isset( $_POST['first_name'] ) ? sanitize_text_field( wp_unslash( $_POST['first_name'] ) ) : '';
		$last_name  = isset( $_POST['last_name'] ) ? sanitize_text_field( wp_unslash( $_POST['last_name'] ) ) : '';
		$pin        = isset( $_POST['pin'] ) ? sanitize_text_field( wp_unslash( $_POST['pin'] ) ) : '';

		if ( empty( $user_id ) || empty( $first_name ) || empty( $last_name ) ) {
			wp_send_json_error( array( 'message' => __( 'Name fields are required.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$existing = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT id FROM {$wpdb->prefix}onetill_users WHERE id = %d",
				$user_id
			)
		);

		if ( ! $existing ) {
			wp_send_json_error( array( 'message' => __( 'User not found.', 'onetill' ) ), 404 );
		}

		$data   = array(
			'first_name' => $first_name,
			'last_name'  => $last_name,
			'updated_at' => current_time( 'mysql', true ),
		);
		$format = array( '%s', '%s', '%s' );

		// Only update PIN if provided.
		if ( ! empty( $pin ) ) {
			if ( ! preg_match( '/^\d{4}$/', $pin ) ) {
				wp_send_json_error( array( 'message' => __( 'PIN must be exactly 4 digits.', 'onetill' ) ), 400 );
			}

			if ( $this->is_pin_taken( $pin, $user_id ) ) {
				wp_send_json_error( array( 'message' => __( 'This PIN is already in use. Please choose a different one.', 'onetill' ) ), 409 );
			}

			$data['pin']        = wp_hash_password( $pin );
			$data['pin_sha256'] = hash( 'sha256', $pin );
			$format[]           = '%s';
			$format[]           = '%s';
		}

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$wpdb->update(
			$wpdb->prefix . 'onetill_users',
			$data,
			array( 'id' => $user_id ),
			$format,
			array( '%d' )
		);

		wp_send_json_success( array(
			'message' => __( 'User updated.', 'onetill' ),
			'user'    => array(
				'id'         => $user_id,
				'first_name' => $first_name,
				'last_name'  => $last_name,
			),
		) );
	}

	/**
	 * AJAX handler: Delete a user.
	 */
	public function ajax_delete_user() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$user_id = isset( $_POST['user_id'] ) ? absint( $_POST['user_id'] ) : 0;

		if ( empty( $user_id ) ) {
			wp_send_json_error( array( 'message' => __( 'Missing user ID.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$deleted = $wpdb->delete(
			$wpdb->prefix . 'onetill_users',
			array( 'id' => $user_id ),
			array( '%d' )
		);

		if ( ! $deleted ) {
			wp_send_json_error( array( 'message' => __( 'User not found.', 'onetill' ) ), 404 );
		}

		wp_send_json_success( array( 'message' => __( 'User deleted.', 'onetill' ) ) );
	}

	/**
	 * AJAX handler: Disconnect a device.
	 */
	public function ajax_disconnect_device() {
		check_ajax_referer( 'onetill_admin', 'nonce' );

		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_send_json_error( array( 'message' => __( 'Permission denied.', 'onetill' ) ), 403 );
		}

		$device_id = isset( $_POST['device_id'] ) ? sanitize_text_field( wp_unslash( $_POST['device_id'] ) ) : '';

		if ( empty( $device_id ) ) {
			wp_send_json_error( array( 'message' => __( 'Missing device ID.', 'onetill' ) ), 400 );
		}

		global $wpdb;

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$device = $wpdb->get_row(
			$wpdb->prepare(
				"SELECT api_key_id FROM {$wpdb->prefix}onetill_devices WHERE id = %s",
				$device_id
			),
			ARRAY_A
		);

		if ( ! $device ) {
			wp_send_json_error( array( 'message' => __( 'Device not found.', 'onetill' ) ), 404 );
		}

		// Revoke the WooCommerce API key.
		if ( ! empty( $device['api_key_id'] ) ) {
			// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- WC API keys table, no public API to revoke.
			$wpdb->delete(
				$wpdb->prefix . 'woocommerce_api_keys',
				array( 'key_id' => $device['api_key_id'] ),
				array( '%d' )
			);
		}

		// phpcs:ignore WordPress.DB.DirectDatabaseQuery.DirectQuery, WordPress.DB.DirectDatabaseQuery.NoCaching -- custom table, no WP API available.
		$wpdb->delete(
			$wpdb->prefix . 'onetill_devices',
			array( 'id' => $device_id ),
			array( '%s' )
		);

		wp_send_json_success( array( 'message' => __( 'Device disconnected.', 'onetill' ) ) );
	}

	/**
	 * Handle saving the Stripe secret key from admin form.
	 */
	public function save_stripe_key() {
		if ( ! current_user_can( 'manage_woocommerce' ) ) {
			wp_die( esc_html__( 'Permission denied.', 'onetill' ) );
		}

		check_admin_referer( 'onetill_save_stripe_key', 'onetill_stripe_nonce' );

		$key = isset( $_POST['onetill_stripe_secret_key'] ) ? sanitize_text_field( wp_unslash( $_POST['onetill_stripe_secret_key'] ) ) : '';
		update_option( 'onetill_stripe_secret_key', $key );

		wp_safe_redirect( admin_url( 'admin.php?page=onetill-settings&stripe_saved=1' ) );
		exit;
	}

	/**
	 * Render the barcode field on the product Inventory tab.
	 */
	public function render_barcode_field() {
		woocommerce_wp_text_input( array(
			'id'          => '_onetill_barcode',
			'label'       => __( 'Barcode / UPC / EAN', 'onetill' ),
			'desc_tip'    => true,
			'description' => __( 'Barcode for POS scanning. Supports EAN-13, UPC-A, or any string.', 'onetill' ),
			'placeholder' => __( 'e.g. 0123456789012', 'onetill' ),
		) );
	}

	/**
	 * Save the barcode field value on product save.
	 *
	 * @param int $post_id The product post ID.
	 */
	public function save_barcode_field( $post_id ) {
		// Nonce is already verified by WooCommerce in woocommerce_process_product_meta.
		// phpcs:ignore WordPress.Security.NonceVerification.Missing
		if ( isset( $_POST['_onetill_barcode'] ) ) {
			update_post_meta(
				$post_id,
				'_onetill_barcode',
				// phpcs:ignore WordPress.Security.NonceVerification.Missing
				sanitize_text_field( wp_unslash( $_POST['_onetill_barcode'] ) )
			);
		}
	}

	/**
	 * Render the barcode field for each variation.
	 *
	 * @param int      $loop           The variation loop index.
	 * @param array    $variation_data The variation data.
	 * @param \WP_Post $variation      The variation post object.
	 */
	public function render_variation_barcode_field( $loop, $variation_data, $variation ) {
		$value = get_post_meta( $variation->ID, '_onetill_barcode', true );
		?>
		<p class="form-row form-row-full">
			<label for="onetill_barcode_<?php echo esc_attr( $loop ); ?>">
				<?php esc_html_e( 'Barcode / UPC / EAN', 'onetill' ); ?>
			</label>
			<input type="text"
				id="onetill_barcode_<?php echo esc_attr( $loop ); ?>"
				name="onetill_variation_barcode[<?php echo esc_attr( $loop ); ?>]"
				value="<?php echo esc_attr( $value ); ?>"
				placeholder="<?php esc_attr_e( 'e.g. 0123456789012', 'onetill' ); ?>"
			/>
		</p>
		<?php
	}

	/**
	 * Save the variation barcode field value.
	 *
	 * @param int $variation_id The variation ID.
	 * @param int $loop_index   The variation loop index.
	 */
	public function save_variation_barcode_field( $variation_id, $loop_index ) {
		// Nonce is already verified by WooCommerce in woocommerce_save_product_variation.
		// phpcs:ignore WordPress.Security.NonceVerification.Missing
		if ( isset( $_POST['onetill_variation_barcode'][ $loop_index ] ) ) {
			update_post_meta(
				$variation_id,
				'_onetill_barcode',
				// phpcs:ignore WordPress.Security.NonceVerification.Missing
				sanitize_text_field( wp_unslash( $_POST['onetill_variation_barcode'][ $loop_index ] ) )
			);
		}
	}

	/**
	 * Get all connected devices.
	 *
	 * @return array List of device records.
	 */
	private function get_devices() {
		global $wpdb;

		return $wpdb->get_results(
			"SELECT * FROM {$wpdb->prefix}onetill_devices ORDER BY paired_at DESC",
			ARRAY_A
		);
	}

	/**
	 * Get change log stats for the dashboard.
	 *
	 * @return array { last_hour, last_24h }
	 */
	private function get_change_log_stats() {
		global $wpdb;

		$last_hour = (int) $wpdb->get_var(
			$wpdb->prepare(
				"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
				gmdate( 'Y-m-d H:i:s', time() - HOUR_IN_SECONDS )
			)
		);

		$last_24h = (int) $wpdb->get_var(
			$wpdb->prepare(
				"SELECT COUNT(*) FROM {$wpdb->prefix}onetill_change_log WHERE timestamp > %s",
				gmdate( 'Y-m-d H:i:s', time() - DAY_IN_SECONDS )
			)
		);

		return array(
			'last_hour' => $last_hour,
			'last_24h'  => $last_24h,
		);
	}

	/**
	 * Get all OneTill users.
	 *
	 * @return array List of user records.
	 */
	private function get_users() {
		global $wpdb;

		return $wpdb->get_results(
			"SELECT id, first_name, last_name, created_at FROM {$wpdb->prefix}onetill_users ORDER BY first_name ASC, last_name ASC",
			ARRAY_A
		);
	}

	/**
	 * Check if a PIN is already in use by another user.
	 *
	 * @param string $pin     The plaintext PIN to check.
	 * @param int    $exclude Optional user ID to exclude (for updates).
	 * @return bool
	 */
	private function is_pin_taken( $pin, $exclude = 0 ) {
		global $wpdb;

		$query = "SELECT id, pin FROM {$wpdb->prefix}onetill_users";
		if ( $exclude ) {
			$query = $wpdb->prepare(
				"SELECT id, pin FROM {$wpdb->prefix}onetill_users WHERE id != %d",
				$exclude
			);
		}

		$users = $wpdb->get_results( $query, ARRAY_A ); // phpcs:ignore WordPress.DB.PreparedSQL.NotPrepared

		foreach ( $users as $user ) {
			if ( wp_check_password( $pin, $user['pin'] ) ) {
				return true;
			}
		}

		return false;
	}
}
