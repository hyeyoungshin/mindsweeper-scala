import scala.util.Random

enum GameStatus:
  case Win
  case Lose
  case Continue

case class GameState (val solution_board: SolutionBoard, 
                      val player_board: PlayerBoard, 
                      val status: GameStatus)

val BOARD_SIZE = (5, 5)
val NUM_MINES = 5

def new_game(): GameState = 
  val mine_locations = generate_mine_locations(NUM_MINES, BOARD_SIZE)
  val mine_board = create_mineboard(mine_locations)
  val solution_board = create_solutionboard(mine_board)
  val initial_board = create_playerboard(BOARD_SIZE._1, BOARD_SIZE._2)
  
  GameState(solution_board, initial_board, GameStatus.Continue)


def game_over(state: GameState): Boolean = 
  state.status match {
    case GameStatus.Lose => true
    case GameStatus.Win => true
    case GameStatus.Continue => false
  }


// TODO: add enum UserAction: case Reveal, Flag
// It isn't model's job to handle user input here
def play(state: GameState, tile_pos: Coordinate, reveal_or_flag: String): GameState = 
  state.status match {
      case GameStatus.Continue => 
        val cur_playerboard = reveal_or_flag match {
          // TODO: remove "R" and "F" and use internal representation
          case "R" => reveal(state.solution_board, state.player_board, tile_pos) 
          case "F" => flag(state.player_board, tile_pos)
          case _ => state.player_board
        }
        update_state(state, cur_playerboard, tile_pos)
      case _ => throw IllegalStateException()
    }


def has_won(solution_board: SolutionBoard, player_board: PlayerBoard): Boolean = 
  val num_mines = solution_board.tile_map.count((_, tile) => tile == SolutionTile.Mine)
  val num_hidden = player_board.tile_map.count((_, tile) => tile == PlayerTile.Hidden)

  num_hidden == num_mines


def update_state(state: GameState, new_player_board: PlayerBoard, tile_pos: Coordinate): GameState = 
  val new_status = 
    if has_won(state.solution_board, new_player_board) then
      GameStatus.Win
    else
      new_player_board.tile_map(tile_pos) match {
        case PlayerTile.Revealed(SolutionTile.Mine) => GameStatus.Lose
        case PlayerTile.Revealed(SolutionTile.Empty) => GameStatus.Continue
        case PlayerTile.Revealed(SolutionTile.Hint(n)) => GameStatus.Continue
        case PlayerTile.Flagged => GameStatus.Continue
        case _ => throw IllegalStateException("tile cannot be hidden.")
      }
    
  GameState(state.solution_board, new_player_board, new_status)


// TODO: User Internal Representation rather than Arrays and Strings 
// Use Coordinate and Board (skip Array[Array[Int]])
// MineBoard as return type
// Move to Board.scala
def generate_mine_locations(num_mines: Int, board_size: (Int, Int)): Array[Array[Int]] =
  var board = Array.fill(board_size._1)(Array.fill(board_size._2)(0))
  val random_coordinates = Random.shuffle(generate_coordinate_keys(board_size._1, board_size._2))
  
  val mine_locations = random_coordinates.take(num_mines)
  mine_locations.foreach((x, y) => board(x)(y) = 1)
  board


// TODO:
// Separate Game(model) and text ui(view).
// Game - Coordinate || text ui - InputCoordinate
// To clean up, we rebuilt via Metals
// Had nested sbt files 


// // tiles and mines ratio
// enum Difficulty:
//   case Easy //  12.6%
//   case Intermediate // 18.1%
//   case Expert // 20.6%


// def flagged_equals_mines(solution_board: SolutionBoard, player_board: PlayerBoard): Boolean = 
  // val mines_pos = solution_board.tile_map.filter((pos, tile) => tile == SolutionTile.Mine).keys
  // mines_pos.foldLeft(true)((acc, pos) => acc && player_board.tile_map(pos) == PlayerTile.Flagged
  
